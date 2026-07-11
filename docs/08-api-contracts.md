# 08 — API Contracts

**Phase:** 5 · **Status:** Draft v1 · **Depends on:** 07-database-design.md

Base URL: `https://api.atlas.app/v1`. Auth: Bearer JWT (see `11-security.md`).
All bodies are JSON. All list endpoints are paginated (`?page=&pageSize=`,
cursor-based for high-volume tables like `audit_logs`).

---

## 8.1 Conventions

- Resource-oriented REST; nested resources reflect the aggregate roots in
  `04-domain-model-relationships.md` (e.g., tasks live under
  `/projects/:projectId/tasks`, not top-level, because Project is the
  aggregate root).
- Standard error shape:
```json
{
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Project not found",
    "details": {}
  }
}
```
- Every mutating endpoint requires the caller's Membership/ProjectMembership
  role to satisfy the permission table in §8.7.

## 8.2 Organizations

```
POST   /organizations                      Create org (caller becomes OWNER)
GET    /organizations/:id                  Get org
PATCH  /organizations/:id                  Update org (ADMIN+)
DELETE /organizations/:id                  Archive org (OWNER only; blocked by business rules)
GET    /organizations/:id/members          List members (paginated)
POST   /organizations/:id/transfer-ownership   Transfer ownership (OWNER only, requires re-auth)
```

**POST /organizations — Request**
```json
{ "name": "Acme Inc", "slug": "acme", "timezone": "Asia/Kolkata", "country": "IN" }
```
**Response 201**
```json
{ "id": "...", "name": "Acme Inc", "slug": "acme", "status": "ACTIVE", "createdAt": "..." }
```
**Errors:** `409 SLUG_TAKEN`, `422 VALIDATION_ERROR`.

## 8.3 Invitations & Membership

```
POST   /organizations/:id/invitations       Create invitation (ADMIN+, checks seat limit)
GET    /organizations/:id/invitations        List pending invitations
DELETE /invitations/:id                      Revoke
POST   /invitations/:id/accept                Accept (authenticated user, or triggers registration)
PATCH  /memberships/:id                       Change role (ADMIN+, cannot set OWNER)
DELETE /memberships/:id                       Remove member (ADMIN+; cannot remove OWNER)
```

**Errors:** `403 SEAT_LIMIT_EXCEEDED`, `410 INVITATION_EXPIRED`,
`409 ALREADY_MEMBER`.

## 8.4 Projects

```
POST   /organizations/:orgId/projects           Create (ADMIN+ or per org setting: any member)
GET    /organizations/:orgId/projects            List (visibility-filtered)
GET    /projects/:id                              Get
PATCH  /projects/:id                              Update (PM+)
DELETE /projects/:id                              Archive (PM+)
POST   /projects/:id/members                       Add member/guest (PM+)
DELETE /projects/:id/members/:userId               Remove member (PM+)
```

## 8.5 Tasks

```
POST   /projects/:projectId/tasks                 Create
GET    /projects/:projectId/tasks                  List (filter: status, assignee, label, milestone)
GET    /tasks/:id                                    Get (includes comments count, attachments)
PATCH  /tasks/:id                                    Update (status, title, description, priority, dueDate)
DELETE /tasks/:id                                    Soft-delete
POST   /tasks/:id/assignees                           Add assignee
DELETE /tasks/:id/assignees/:userId                    Remove assignee
POST   /tasks/:id/labels                                Add label
DELETE /tasks/:id/labels/:labelId                        Remove label
```

**PATCH /tasks/:id — Request**
```json
{ "status": "DONE" }
```
**Response 200**
```json
{ "id": "...", "key": "ATL-142", "status": "DONE", "completedAt": "2026-07-11T09:00:00Z", "updatedAt": "..." }
```
Side effects (see `09-event-flows.md`): emits `task.status_changed` event
→ Notification, ActivityEntry, SearchIndex update, Automation evaluation.

## 8.6 Comments & Attachments

```
POST   /tasks/:taskId/comments                Create (parses @mentions)
GET    /tasks/:taskId/comments                 List (paginated, oldest-first)
PATCH  /comments/:id                             Edit (author or PM+, <15min window flags no "(edited)")
DELETE /comments/:id                             Delete (author or PM+)
POST   /tasks/:taskId/attachments                 Upload (multipart, returns pending scan status)
POST   /comments/:commentId/attachments            Upload
GET    /attachments/:id/download                    Download (403 if scanStatus != CLEAN)
```

## 8.7 Billing

```
GET    /organizations/:id/subscription          Get current plan/usage
POST   /organizations/:id/subscription/checkout   Create provider checkout session
POST   /organizations/:id/subscription/change      Change plan (validates seat/feature limits first)
POST   /webhooks/stripe                             Provider webhook (signature-verified, not user-facing)
```

## 8.8 Audit & Activity (read-only)

```
GET  /organizations/:id/audit-logs      (OWNER/ADMIN only; cursor-paginated, filterable by actor/target/date)
GET  /organizations/:id/activity         (any member; project/task-scoped filters)
```

## 8.9 Permission Matrix (endpoint category → minimum role)

| Endpoint category | OWNER | ADMIN | BILLING_MGR | PM | MEMBER | GUEST |
|---|---|---|---|---|---|---|
| Org settings write | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| Org delete | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Billing read/write | ✅ | ❌ | ✅ | ❌ | ❌ | ❌ |
| Invite org members | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| Create project | ✅ | ✅ | ❌ | ✅* | ❌ | ❌ |
| Project settings write | ✅ | ✅ | ❌ | ✅ (own project) | ❌ | ❌ |
| Create/assign task | ✅ | ✅ | ❌ | ✅ | ✅ (own project) | ❌ |
| Edit own task/comment | ✅ | ✅ | ❌ | ✅ | ✅ | ✅ |
| View audit logs | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |

\* Configurable per-org setting: `allowMembersToCreateProjects`.

## 8.10 Versioning Policy

- Breaking changes require a new version prefix (`/v2/...`); `/v1` is
  supported for a minimum 6 months after `/v2` GA.
- Additive changes (new optional field, new endpoint) do not bump version
  but must be noted in this doc's changelog.

---
*Changelog*
- v1.0 — Core CRUD surface for all v1 modules defined with permission matrix.

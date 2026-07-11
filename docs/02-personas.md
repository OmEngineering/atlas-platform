# 02 — User Personas

**Phase:** 1 · **Status:** Draft v1 · **Depends on:** 01-product-vision.md

Every permission decision in `05-entity-specifications.md` and every API
scope in `08-api-contracts.md` traces back to one of these personas.

---

## 2.1 Persona Table

| Persona | Scope | Can do | Cannot do |
|---|---|---|---|
| **Super Admin** | Platform-wide (Atlas staff only) | Manage all organizations, impersonate for support, view platform health | Cannot bypass audit logging; impersonation itself is audited |
| **Organization Owner** | Single org | Full control: billing, delete org, transfer ownership, manage all members/projects | There is exactly one Owner per org at a time |
| **Organization Admin** | Single org | Manage members, teams, projects, settings | Cannot manage billing or delete the org |
| **Project Manager** | Single project (or set of projects) | Create/edit tasks, milestones, manage project members, assign work | Cannot manage org-level settings or billing |
| **Developer / Member** | Assigned projects | Create/edit own tasks, comment, upload attachments, view assigned work | Cannot manage members, cannot delete others' tasks |
| **Guest** | Single project, time-limited | View project, comment, limited attachment upload | Cannot see other projects, cannot invite others, cannot see org member list |
| **Billing Manager** | Org billing only | View/change subscription, payment method, invoices | Cannot access projects/tasks unless also granted another role |

## 2.2 Persona Detail

### Super Admin
- Internal Atlas staff role, not org-assignable.
- Used for support/debugging; every impersonation session is written to
  `AuditLog` with `actorType = SUPER_ADMIN`.

### Organization Owner
- Created automatically as the user who runs org creation (see
  `06-user-journeys.md` §Registration → Org Creation).
- Business rule: **Owner cannot leave the organization** without first
  transferring ownership (see `05-entity-specifications.md` → Organization
  → Business Rules).

### Organization Admin
- Day-to-day org management: invite/remove members, create teams, set org
  defaults.
- Cannot touch `Subscription`/`Billing` entities — that's Billing Manager
  or Owner only.

### Project Manager
- Scoped to specific `Project`s via `Membership`.
- Can create `Milestone`s, assign `Task`s, manage the project's `Label`
  taxonomy.

### Developer / Member
- The default role granted on project invitation.
- Full CRUD on tasks/comments they own; read on everything else in their
  assigned projects.

### Guest
- Created via project-scoped invitation with an expiry (`Invitation.expiresAt`).
- Cannot see the org's member directory, other projects, or billing.
- Intended for external contractors/clients.

### Billing Manager
- A role that can be layered onto any member (e.g., an Admin who's also
  Billing Manager) or granted narrowly to someone (e.g., a finance
  contact) with no project access at all.

## 2.3 Role Hierarchy (for permission inheritance)

```
Super Admin (platform)
   │
   └── does not inherit org roles; separate plane

Organization Owner
   │  (implies all Admin permissions)
   └── Organization Admin
          │  (implies all PM permissions within managed projects)
          └── Project Manager
                 │
                 └── Developer / Member
                        │
                        └── Guest (not inherited — assigned directly, most restrictive)

Billing Manager — orthogonal to the hierarchy above, can be combined with any role
```

## 2.4 Persona → Primary Journeys (cross-reference)

| Persona | Primary journeys in `06-user-journeys.md` |
|---|---|
| Organization Owner | Registration & Org Creation, Billing Setup, Ownership Transfer |
| Organization Admin | Member Invitation, Team Creation |
| Project Manager | Project Creation, Task Assignment, Milestone Planning |
| Developer | Task Execution, Commenting, Attachment Upload |
| Guest | Limited Project View, Commenting |
| Billing Manager | Subscription Change, Payment Method Update |

---
*Changelog*
- v1.0 — Initial personas drafted; expanded Owner/Admin split.

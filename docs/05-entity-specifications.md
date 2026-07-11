# 05 — Entity Specifications

**Phase:** 2 · **Status:** Draft v1 · **Depends on:** 04-domain-model-relationships.md

Every entity below follows the same template: **Fields → Relationships →
Business Rules → Indexes**. This is the contract that `07-database-design.md`
and `08-api-contracts.md` are generated from — don't add a field here
without also updating those docs in the same PR.

---

## 5.1 Organization

**Fields**
| Field | Type | Notes |
|---|---|---|
| id | uuid | PK |
| name | string | Display name |
| slug | string | URL-safe, unique |
| logoUrl | string, nullable | |
| description | text, nullable | |
| timezone | string | IANA tz, default `UTC` |
| country | string (ISO 3166-1 alpha-2) | |
| subscriptionId | uuid, FK → Subscription | 1:1 |
| status | enum(`ACTIVE`,`SUSPENDED`,`ARCHIVED`) | |
| createdBy | uuid, FK → User | |
| createdAt / updatedAt / deletedAt | timestamp | soft-delete via `deletedAt` |

**Relationships:** 1 Organization → N Membership, N Project, N Team, N Invitation; 1:1 Subscription.

**Business Rules**
- Cannot delete (archive) if `Subscription.status = ACTIVE`.
- Cannot archive if any `Project.status != ARCHIVED` exists.
- Exactly one Membership with `role = OWNER` must exist at all times; the
  Owner cannot leave (must transfer ownership first).
- `slug` is globally unique, immutable after creation.
- `name` uniqueness is **not** enforced globally (two orgs can share a
  display name), only `slug` must be unique.

**Indexes:** unique(`slug`), index(`status`), index(`createdBy`).

---

## 5.2 User

**Fields**
| Field | Type | Notes |
|---|---|---|
| id | uuid | PK |
| email | string | unique, lowercase-normalized |
| passwordHash | string, nullable | null if SSO-only |
| fullName | string | |
| avatarUrl | string, nullable | |
| emailVerifiedAt | timestamp, nullable | |
| status | enum(`ACTIVE`,`DEACTIVATED`) | |
| lastLoginAt | timestamp, nullable | |
| createdAt / updatedAt | timestamp | |

**Relationships:** 1 User → N Membership (across orgs); N ProjectMembership.

**Business Rules**
- Cannot hard-delete a User who is `OWNER` of any active Organization.
- Deactivating a User does not delete their authored Tasks/Comments —
  authorship is preserved, UI falls back to "Deactivated User".
- `email` must be verified (`emailVerifiedAt` not null) before the user can
  create or join an Organization.

**Indexes:** unique(`email`), index(`status`).

---

## 5.3 Membership (Organization ↔ User)

**Fields**
| Field | Type | Notes |
|---|---|---|
| id | uuid | PK |
| organizationId | uuid, FK | |
| userId | uuid, FK | |
| role | enum(`OWNER`,`ADMIN`,`BILLING_MANAGER`,`MEMBER`) | see `02-personas.md` |
| invitedBy | uuid, FK → User, nullable | null if org creator |
| joinedAt | timestamp | |
| status | enum(`ACTIVE`,`REMOVED`) | soft-remove |

**Relationships:** N:1 Organization, N:1 User.

**Business Rules**
- Unique(`organizationId`, `userId`) — one membership row per user per org.
- Exactly one `ACTIVE` row with `role = OWNER` per organization (enforced
  via application transaction, mirrored by a DB constraint trigger — see
  `07-database-design.md`).
- `role` transitions to `OWNER` only via the explicit "Transfer Ownership"
  operation, never a generic role-update endpoint.

**Indexes:** unique(`organizationId`,`userId`), index(`userId`).

---

## 5.4 Team

**Fields**: id, organizationId (FK), name, description (nullable), createdBy (FK User), createdAt/updatedAt.

**Join table `TeamMember`**: id, teamId (FK), userId (FK), addedAt.

**Business Rules**
- `name` unique within an organization.
- A user must have an active org Membership to be added to a Team.

**Indexes:** unique(`organizationId`,`name`), unique(`teamId`,`userId`) on join table.

---

## 5.5 Invitation

**Fields**
| Field | Type | Notes |
|---|---|---|
| id | uuid | PK |
| organizationId | uuid, FK | |
| projectId | uuid, FK, nullable | null = org-level invite |
| email | string | invitee |
| role | enum | role granted on acceptance |
| token | string | random, hashed at rest |
| invitedBy | uuid, FK → User | |
| status | enum(`PENDING`,`ACCEPTED`,`EXPIRED`,`REVOKED`) | |
| expiresAt | timestamp | default now()+7d |
| createdAt | timestamp | |

**Business Rules**
- `projectId` set ⇒ acceptance creates a `ProjectMembership` with role
  `GUEST` or `MEMBER` only, never an org-level role.
- `projectId` null ⇒ acceptance creates an org `Membership`; role cannot
  be `OWNER` (ownership is never granted via invitation).
- Expired invitations cannot be accepted; re-inviting the same email
  revokes the prior pending invitation.

**Indexes:** index(`organizationId`,`email`), index(`token`) unique.

---

## 5.6 Project

**Fields**: id, organizationId (FK), name, key (short code, e.g. `ATL`, unique per org), description, status (`ACTIVE`,`ARCHIVED`), visibility (`ORG_WIDE`,`RESTRICTED`), createdBy (FK), createdAt/updatedAt/deletedAt.

**Business Rules**
- `key` unique within org, immutable, used as the human-readable task
  prefix (`ATL-142`).
- `visibility = RESTRICTED` ⇒ only explicit `ProjectMembership` rows can
  see it; `ORG_WIDE` ⇒ all org members have read access, explicit
  membership only needed to be assigned work.
- Archiving a project cascades a soft-archive to its Tasks (status
  unaffected, just hidden from default views).

**Indexes:** unique(`organizationId`,`key`), index(`organizationId`,`status`).

---

## 5.7 ProjectMembership

**Fields**: id, projectId (FK), userId (FK), role (`PM`,`MEMBER`,`GUEST`), addedAt, addedBy (FK).

**Business Rules**
- Unique(`projectId`,`userId`).
- `GUEST` role membership rows must have originated from a project-scoped
  `Invitation` (enforced at the service layer, not DB).

**Indexes:** unique(`projectId`,`userId`).

---

## 5.8 Task

**Fields**
| Field | Type | Notes |
|---|---|---|
| id | uuid | PK |
| projectId | uuid, FK | |
| milestoneId | uuid, FK, nullable | |
| key | string | `<Project.key>-<seq>`, e.g. `ATL-142` |
| title | string | |
| description | text, nullable | markdown |
| status | enum(`TODO`,`IN_PROGRESS`,`IN_REVIEW`,`DONE`,`CANCELLED`) | |
| priority | enum(`LOW`,`MEDIUM`,`HIGH`,`URGENT`) | |
| dueDate | date, nullable | |
| createdBy | uuid, FK → User | |
| createdAt / updatedAt / deletedAt | timestamp | |

**Join tables:** `TaskAssignee` (taskId, userId), `TaskLabel` (taskId, labelId).

**Business Rules**
- `key` is generated server-side, sequential per project, immutable.
- An assignee must have an active `ProjectMembership` on the task's
  project.
- Moving to `DONE` sets a `completedAt` timestamp (used by Automation/
  Analytics later); moving off `DONE` clears it.
- Soft-deleted tasks are excluded from Search index within 5 minutes
  (event-driven, see `09-event-flows.md`).

**Indexes:** unique(`projectId`,`key`), index(`projectId`,`status`), index(`milestoneId`).

---

## 5.9 Milestone

**Fields**: id, projectId (FK), name, description, startDate, dueDate, status (`PLANNED`,`ACTIVE`,`COMPLETED`), createdAt/updatedAt.

**Business Rules**
- `dueDate >= startDate`, enforced at API layer.
- Deleting a milestone nulls `milestoneId` on associated tasks (see
  `04-domain-model-relationships.md` §4.3).

**Indexes:** index(`projectId`,`status`).

---

## 5.10 Label

**Fields**: id, organizationId (FK), projectId (FK, nullable — null = org-wide label), name, color (hex), createdAt.

**Business Rules**
- Unique(`organizationId`,`projectId`,`name`) — project-scoped labels can
  share a name with an org-wide label without conflict since `projectId`
  differs.

**Indexes:** unique(`organizationId`,`projectId`,`name`).

---

## 5.11 Comment

**Fields**: id, taskId (FK), authorId (FK User), body (text, markdown), createdAt/updatedAt/deletedAt.

**Business Rules**
- Only the author or a user with `PM`+ role on the project can edit/delete.
- Edits beyond a 15-minute window are marked `(edited)` in the API
  response (`editedAt` populated).
- `@mentions` parsed from `body` at write-time and fan out to
  Notifications (see `09-event-flows.md`).

**Indexes:** index(`taskId`,`createdAt`).

---

## 5.12 Attachment

**Fields**: id, taskId (FK, nullable), commentId (FK, nullable), uploadedBy (FK User), fileName, mimeType, sizeBytes, storageKey (S3 object key), scanStatus (`PENDING`,`CLEAN`,`INFECTED`), createdAt.

**Business Rules**
- Exactly one of `taskId`/`commentId` set, never both, never neither.
- `scanStatus != CLEAN` ⇒ file is not downloadable (see `11-security.md`
  §Upload Scanning).
- Max size enforced per plan tier (see Subscription).

**Indexes:** index(`taskId`), index(`commentId`).

---

## 5.13 AuditLog

**Fields**: id, organizationId (FK, nullable for platform-level events), actorId (FK User, nullable for system events), actorType (`USER`,`SYSTEM`,`SUPER_ADMIN`), action (string, e.g. `project.archived`), targetType, targetId, metadata (jsonb), ipAddress, createdAt.

**Business Rules**
- **Append-only.** No UPDATE or DELETE ever, enforced via DB
  permissions (application's DB role has no UPDATE/DELETE grant on this
  table).
- Retained indefinitely in v1 (retention policy TBD in `13-roadmap.md`).

**Indexes:** index(`organizationId`,`createdAt`), index(`targetType`,`targetId`).

---

## 5.14 Subscription

**Fields**: id, organizationId (FK, unique), plan (`FREE`,`TEAM`,`BUSINESS`,`ENTERPRISE`), seats, status (`ACTIVE`,`PAST_DUE`,`CANCELLED`), providerCustomerId, providerSubscriptionId, currentPeriodEnd, createdAt/updatedAt.

**Business Rules**
- `seats` cannot be reduced below current `ACTIVE` Membership count.
- `status = PAST_DUE` for >14 days ⇒ Organization auto-transitions to
  `SUSPENDED` (read-only mode, see Organization business rules).

**Indexes:** unique(`organizationId`), index(`status`).

---
*Changelog*
- v1.0 — 14 core entities specified with fields, rules, indexes.

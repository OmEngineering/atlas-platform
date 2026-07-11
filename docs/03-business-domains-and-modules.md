# 03 — Business Domains & Modules

**Phase:** 2 · **Status:** Draft v1 · **Depends on:** 02-personas.md

---

## 3.1 Domain List

| # | Module | Core purpose | Owning bounded context |
|---|---|---|---|
| 1 | Organization | Tenant boundary, org profile & settings | Identity & Tenancy |
| 2 | Users & Auth | Accounts, sessions, email verification | Identity & Tenancy |
| 3 | Membership | User ↔ Organization/Project role binding | Identity & Tenancy |
| 4 | Teams | Grouping of members within an org | Identity & Tenancy |
| 5 | Invitations | Time-boxed onboarding of new members/guests | Identity & Tenancy |
| 6 | Projects | Unit of work container | Work Management |
| 7 | Tasks | Actionable work items | Work Management |
| 8 | Milestones | Time-boxed goals within a project | Work Management |
| 9 | Labels | Cross-cutting tags for tasks | Work Management |
| 10 | Comments | Threaded discussion on tasks | Collaboration |
| 11 | Attachments | File uploads on tasks/comments | Collaboration |
| 12 | Activity Timeline | Human-readable feed of what happened | Collaboration |
| 13 | Notifications | Push/email/in-app alerts derived from events | Engagement |
| 14 | Audit Logs | Immutable record of security-relevant actions | Compliance |
| 15 | Billing & Subscriptions | Plan, payment, usage limits | Commerce |
| 16 | Calendar | Deadline/milestone visualization | Work Management |
| 17 | Automation | Rule-based triggers ("when X, do Y") | Engagement |
| 18 | Search | Cross-entity search index | Platform |

## 3.2 Bounded Contexts (grouping of modules)

```
Identity & Tenancy   → Organization, Users & Auth, Membership, Teams, Invitations
Work Management      → Projects, Tasks, Milestones, Labels, Calendar
Collaboration        → Comments, Attachments, Activity Timeline
Engagement           → Notifications, Automation
Compliance           → Audit Logs
Commerce             → Billing & Subscriptions
Platform             → Search
```

These contexts are the seams along which `10-architecture.md` proposes
future service extraction (Modular Monolith → Notification Service →
Search Service → Billing Service → Analytics Service).

## 3.3 Module Detail

### 3.3.1 Organization
The tenant root. Every other entity except `User` and `AuditLog`
(platform-level) hangs off an `organizationId`. See full spec in
`05-entity-specifications.md`.

### 3.3.2 Users & Auth
Global user accounts (a `User` can belong to multiple organizations via
`Membership`). Handles registration, email verification, password
reset, session/token management.

### 3.3.3 Membership
The join entity between `User` and `Organization` (and separately,
`User` and `Project`), carrying the `role` enum from `02-personas.md`.
This is the enforcement point for every permission check.

### 3.3.4 Teams
Optional sub-grouping of org members (e.g., "Backend Team",
"Design Team") used for bulk-assigning to projects and for
`@mention`-style notifications.

### 3.3.5 Invitations
Tokenized, expiring invites. Two flavors: **org invitation** (becomes an
org Membership) and **project invitation** (becomes a Guest-scoped
project Membership only).

### 3.3.6 Projects
The primary work container. Belongs to exactly one Organization. Has its
own Membership list (subset of org members, plus optional Guests).

### 3.3.7 Tasks
The atomic unit of work. Belongs to a Project, optionally a Milestone,
has assignee(s), Labels, Comments, Attachments, and a status/priority.

### 3.3.8 Milestones
Time-boxed grouping of tasks within a project ("Sprint 4", "Beta
Launch"). Drives Calendar and progress reporting.

### 3.3.9 Labels
Free-form or org-defined tags on tasks, scoped per-project or per-org
(configurable — see business rules in entity spec).

### 3.3.10 Comments
Threaded, belongs to a Task (v1) with room to extend to other entities
later. Supports @mentions which fan out to Notifications.

### 3.3.11 Attachments
File metadata + storage pointer (S3), attachable to Tasks and Comments.
Virus-scanned on upload (see `11-security.md`).

### 3.3.12 Activity Timeline
Denormalized, human-readable projection built from domain events (see
`09-event-flows.md`) — "Priya moved Task #221 to Done."

### 3.3.13 Notifications
Consumes domain events, applies user notification preferences, delivers
via in-app/email/(later push). Candidate for first service extraction.

### 3.3.14 Audit Logs
Append-only, immutable. Every mutating API call on a sensitive entity
(Organization, Membership, Billing, Auth) writes an AuditLog row.
Never updated or deleted, even by Super Admin.

### 3.3.15 Billing & Subscriptions
Tracks the org's plan, seat count, usage against plan limits, and
payment status (via a payment provider — Stripe assumed). Does **not**
do invoicing/ledger accounting itself.

### 3.3.16 Calendar
Read-model over Tasks (due dates) and Milestones — no new source-of-truth
data, mostly a query/aggregation module.

### 3.3.17 Automation
v2+ module (see `13-roadmap.md`): rule engine — "when Task moves to
Done, notify PM" — deferred past v1 minimal scope but domain-modeled now
so Tasks/Notifications don't need reshaping later.

### 3.3.18 Search
Indexes Organizations-scoped Projects/Tasks/Comments for full-text
search. Backed by the event stream, not a synchronous write path.

## 3.4 Module Dependency Graph

```
Organization ──┬── Membership ── Teams
               │
               ├── Projects ──┬── Tasks ──┬── Comments ── Attachments
               │              │           ├── Labels
               │              │           └── Attachments
               │              └── Milestones ── Calendar
               │
               ├── Invitations
               │
               ├── Billing/Subscriptions
               │
               └── AuditLog (write-only sink from everywhere)

Notifications  ← subscribes to events from: Tasks, Comments, Invitations, Membership
Search         ← subscribes to events from: Projects, Tasks, Comments
Activity       ← subscribes to events from: everywhere
Automation     ← subscribes to events, emits: Tasks, Notifications
```

---
*Changelog*
- v1.0 — 18 modules identified and grouped into 6 bounded contexts.

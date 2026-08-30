# 09 — Event Flows

**Phase:** 5 · **Status:** Draft v1 · **Depends on:** 08-api-contracts.md

Broker: RabbitMQ for v1 (topic exchange), with a documented migration path
to Kafka if/when Search + Analytics need replayable log semantics (see
`10-architecture.md` §Architecture Evolution and `13-roadmap.md`).

---

## 9.1 Event Naming Convention

`<entity>.<past_tense_action>` — e.g. `task.created`, `task.status_changed`,
`membership.role_changed`, `invitation.accepted`, `subscription.payment_failed`.

## 9.2 Canonical Event Envelope

```json
{
  "eventId": "uuid",
  "eventType": "task.status_changed",
  "organizationId": "uuid",
  "occurredAt": "2026-07-11T09:00:00Z",
  "actor": { "id": "uuid", "type": "USER" },
  "payload": { "taskId": "...", "from": "IN_REVIEW", "to": "DONE" },
  "version": 1
}
```

## 9.3 Example Flow: Task Created

```
API: POST /projects/:id/tasks  (synchronous write to `tasks` table)
        ↓
Publish `task.created` to RabbitMQ (topic: atlas.tasks)
        ↓                ↓                    ↓                ↓
 Notification        Activity            Search Index      Automation
 Consumer            Consumer            Consumer           Consumer
        ↓                ↓                    ↓                ↓
 Notify watchers   Append ActivityEntry  Upsert doc in    Evaluate rules
 (if assigned)                            search index    ("auto-label", etc.)
```

Each consumer is independent — a Search indexing failure does not block
Notification delivery, and vice versa. Each consumer has its own dead-
letter queue and retry policy (exponential backoff, max 5 attempts, then
DLQ + alert).

## 9.4 Example Flow: Task Status Changed → Done

```
Task Created
   ↓ (already indexed/notified from creation)
Task moves to DONE
   ↓
Publish `task.status_changed`
   ↓
   ├── Notification: assignee + task creator + watchers notified
   ├── Activity: "X marked ATL-142 as Done"
   ├── Search: reindex (status is a searchable/filterable field)
   └── Automation: if rule "on Done, notify PM" exists → emits `notification.requested`
```

## 9.5 Example Flow: Invitation Accepted

```
POST /invitations/:id/accept
        ↓
Transaction: Invitation.status=ACCEPTED, Membership (or ProjectMembership) created
        ↓
Publish `invitation.accepted`
        ↓
   ├── Notification → inviter: "X joined your organization"
   ├── Activity → org-level feed entry
   └── AuditLog → written synchronously in the same DB transaction as the
                   accept operation (audit writes are NOT eventually-consistent;
                   see 11-security.md — audit trail must never be lossy)
```

**Important distinction:** `AuditLog` writes happen **synchronously**,
inside the same transaction as the mutating operation — never via the
async event bus. Everything else (Notification, Activity, Search,
Automation) is eventually consistent via events. This is deliberate: we
can tolerate a delayed search index update; we cannot tolerate a missing
audit trail entry.

## 9.6 Example Flow: Subscription Payment Failed

```
Stripe webhook → POST /webhooks/stripe (signature verified)
        ↓
Subscription.status = PAST_DUE (synchronous write)
        ↓
Publish `subscription.payment_failed`
        ↓
   ├── Notification → Owner + Billing Manager (email, high priority)
   └── AuditLog (synchronous, same transaction)

[Scheduled job, daily]
   ↓
Check: PAST_DUE for >= 14 days?
   ↓ yes
Organization.status = SUSPENDED (synchronous)
   ↓
Publish `organization.suspended`
   ↓
   └── Notification → Owner + Billing Manager
```

## 9.7 Consumer Reliability Rules

- All consumers are **idempotent** — reprocessing the same `eventId` is a
  no-op (dedup table or upsert semantics), because at-least-once delivery
  is assumed.
- Consumers never write back to the tables owned by the publishing
  domain (Notification consumer never writes to `tasks`) — one-way data
  flow, per bounded context (see `03-business-domains-and-modules.md`).
- Every consumer emits its own metrics (`consumed`, `failed`, `dlq`) —
  see `12-infrastructure-deployment.md` §Monitoring.

## 9.8 Full Event Catalog (v1)

| Event | Publishers | Consumers |
|---|---|---|
| `organization.created` | Organizations API | Activity, Audit |
| `organization.suspended` | Scheduled job | Notification, Activity, Audit |
| `membership.created` | Invitations API | Notification, Activity, Audit |
| `membership.role_changed` | Memberships API | Notification, Activity, Audit |
| `membership.ownership_transferred` | Organizations API | Notification, Activity, Audit |
| `invitation.created` / `.accepted` / `.revoked` | Invitations API | Notification, Activity, Audit |
| `project.created` / `.archived` | Projects API | Activity, Search, Audit |
| `task.created` / `.updated` / `.status_changed` / `.deleted` | Tasks API | Notification, Activity, Search, Automation |
| `comment.created` (with mentions) | Comments API | Notification, Activity, Search |
| `subscription.payment_failed` / `.updated` | Billing webhook | Notification, Audit |

---
*Changelog*
- v1.0 — Event envelope, sync-vs-async audit rule, and v1 event catalog defined.

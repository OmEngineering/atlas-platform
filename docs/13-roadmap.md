# 13 — Roadmap

**Phase:** 7 · **Status:** Draft v1 · **Depends on:** all prior docs

---

## 13.1 v1 Scope (Minimal Complete Product)

Everything needed for the end-to-end journey in `06-user-journeys.md`
§6.1, correctly permissioned per `08-api-contracts.md` §8.9:

- [x] Organizations, Users/Auth, Memberships, Invitations
- [x] Projects, ProjectMembership
- [x] Tasks, Milestones, Labels, Comments, Attachments
- [x] Notifications (in-app + email only)
- [ ] Audit Logs
- [ ] Billing (single provider, Stripe, Free/Team/Business plans)
- [ ] Activity Timeline

Explicitly **out** of v1: Teams module UI (entity exists, minimal API
only), Automation, Search (basic Postgres `ILIKE` fallback instead of a
dedicated index), Calendar (derived view only, no dedicated UI).

## 13.2 v2 Candidates

- Teams — full UI, bulk-assign to projects.
- Automation — rule engine ("when X, do Y") per
  `03-business-domains-and-modules.md` §3.3.17.
- SSO (SAML/OIDC) — entity model already supports it (`User.passwordHash`
  nullable), needs provider integration + admin config UI.
- Push notifications (mobile).
- Real Search service extraction, per `10-architecture.md` §10.1 Version 3
  — trigger: `ILIKE` query latency or Postgres load becomes a bottleneck.

## 13.3 v3+ Candidates

- Notification Service extraction (Version 2 in `10-architecture.md`) —
  trigger: notification volume/latency starts affecting core API
  performance, or team wants independent deploy cadence for this module.
- Billing Service extraction (Version 4) — trigger: compliance
  requirement (PCI scope reduction) or second payment provider needed.
- Analytics Service (Version 5) — trigger: reporting/dashboard demand
  from customers exceeds what ad-hoc queries on the OLTP replica can serve.
- Dedicated-tenant deployment option for enterprise customers requiring
  physical data isolation (see `10-architecture.md` §10.5).
- Real-time collaborative document editing (explicitly out of v1 per
  `01-product-vision.md` §1.5).

## 13.4 Deferred Technical Debt (tracked on purpose, not forgotten)

- `audit_logs` partitioning (`07-database-design.md` §7.7) — deferred
  until row count/query latency warrants it.
- Kafka migration for event bus (`10-architecture.md` §10.3) — deferred
  until a consumer needs replay/ordering guarantees RabbitMQ doesn't give.
- Audit log retention policy — currently indefinite; needs a decision
  once storage cost or compliance requirement (e.g. GDPR right-to-
  erasure interaction with an append-only log) forces the question.

## 13.5 Open Questions (decide before the relevant phase starts)

| Question | Affects | Needs decision by |
|---|---|---|
| Single global Stripe account vs. per-region billing entities? | Billing module | Before Billing Service extraction (v3+) |
| GDPR erasure vs. append-only AuditLog — pseudonymize instead of delete? | Audit, Users | Before any EU customer onboarding |
| Guest role: should it ever see the org name/logo, or fully anonymized? | Personas, Projects | Before Guest-heavy customer segment (agencies/contractors) |
| Kafka vs. staying on RabbitMQ long-term? | Architecture | Before Search Service extraction |

## 13.6 How to Propose a Change to This Roadmap

Open a PR editing this file directly (not a design doc issue) — see
`GIT_WORKFLOW.md` §Documentation-Only PRs. Roadmap changes need one
reviewer, no code required.

---
*Changelog*
- v1.0 — v1 scope locked, v2/v3 candidates and deferred debt captured.

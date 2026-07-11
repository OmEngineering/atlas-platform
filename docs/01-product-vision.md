# 01 — Product Vision

**Phase:** 1 · **Status:** Draft v1 · **Depends on:** none

---

## 1.1 What Atlas Is

Atlas is an enterprise collaboration platform for startups and growing
organizations to manage teams, projects, tasks, documents, notifications,
permissions, and workflows — under a single organization-scoped account.

## 1.2 One-Line Pitch

> Atlas is the organization-first workspace where teams plan, build, and
> ship together, without duct-taping five different tools.

## 1.3 Positioning — Why Atlas, Not X

| Competitor | Their center of gravity | Atlas's difference |
|---|---|---|
| Trello | Boards & cards, loosely structured | Atlas is **organization-first**: every project, member, and permission is scoped to an org from day one, not bolted on later. |
| Jira | Deep issue-tracking complexity | Atlas favors **collaboration and simplicity** over ticket-workflow configurability. Fewer knobs, sane defaults. |
| Notion | Flexible docs-as-database, client-heavy | Atlas is **backend-driven and built to scale** — permissions, audit, and billing are enforced server-side, not by document structure. |
| Asana | Task-centric, workflow automation | Atlas treats tasks as one module among many (billing, audit, notifications are first-class, not add-ons). |

## 1.4 Core Product Pillars

1. **Organization-first** — nothing exists outside an organization. No
   "personal workspace" ambiguity.
2. **Permission-native** — every entity has an owner and a visibility rule;
   RBAC is a foundation, not a feature flag.
3. **Auditable by default** — every mutating action is logged. Compliance
   isn't an afterthought.
4. **Composable modules** — Projects, Tasks, Teams, Billing, Notifications
   are independently reasoned-about domains (see `03-business-domains-and-modules.md`)
   that compose, rather than one monolithic "workspace" object.
5. **API-first** — the web client is a consumer of the API, not the source
   of truth. Anything the UI can do, the API can do.

## 1.5 What Atlas Is Not (Explicit Non-Goals for v1)

- Not a general-purpose wiki/notes tool (no Notion-style freeform docs in v1).
- Not a customer-support/helpdesk tool.
- Not a full accounting system — Billing module tracks subscriptions and
  usage, not invoicing/ledgers.
- Not real-time collaborative document editing in v1 (may become a module
  later; see `13-roadmap.md`).

## 1.6 Target Users

Startups (5–200 people) and internal teams at larger orgs who've outgrown
spreadsheets/Trello but find Jira too heavy. Full personas in
`02-personas.md`.

## 1.7 Success Criteria for v1

- A user can register → verify email → create an organization → invite
  members → create a project → create/assign tasks → receive
  notifications, end-to-end, with every action audit-logged.
- Every module in `03-business-domains-and-modules.md` has at least a
  minimal, correctly-permissioned implementation.
- The system is deployable as a single modular monolith (see
  `10-architecture.md` §Architecture Evolution) with clear seams for future
  service extraction.

## 1.8 Guiding Product Principles (used to resolve future disagreements)

1. **Org boundary is sacred.** No cross-org data leakage, ever — this is a
   security principle before it's a product principle.
2. **Simplicity beats configurability** when the two conflict — we add a
   setting only when at least two real personas need different behavior.
3. **Every entity is auditable and soft-deletable** unless there's an
   explicit reason not to be (see `05-entity-specifications.md`).
4. **Backend owns business rules.** The frontend never enforces a rule the
   API doesn't also enforce.

---
*Changelog*
- v1.0 — Initial vision drafted from founder brief.

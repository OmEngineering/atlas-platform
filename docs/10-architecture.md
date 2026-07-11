# 10 — Architecture

**Phase:** 6 · **Status:** Draft v1 · **Depends on:** 09-event-flows.md

---

## 10.1 Architecture Evolution

```
Version 1 — Modular Monolith
   Single deployable, internal module boundaries mirror
   03-business-domains-and-modules.md bounded contexts.
   RabbitMQ used internally for the event flows in doc 09,
   even though everything runs in one process — this is deliberate,
   so extraction later doesn't require rewriting the messaging pattern.
        ↓
Version 2 — Notification Service extracted
   Highest write-fanout, least coupled to core transactional data.
   First candidate because it's easiest to get wrong (spam/missed
   notifications) and benefits most from independent scaling/deploys.
        ↓
Version 3 — Search Service extracted
   Read-heavy, eventually-consistent by design already (doc 09 §9.7),
   benefits from a dedicated index (Elasticsearch/Meilisearch) and
   independent scaling from the transactional DB.
        ↓
Version 4 — Billing Service extracted
   Isolates PCI-adjacent surface area and provider webhook handling
   from the core monolith for security/compliance reasons.
        ↓
Version 5 — Analytics Service
   Aggregates event stream for reporting/dashboards; read replica +
   OLAP-friendly store, decoupled from OLTP path entirely.
```

**Principle:** we do not extract a service until the monolith module
boundary has proven stable for at least one full version — premature
extraction costs more than it saves at this stage (see `13-roadmap.md`
for triggers/thresholds).

## 10.2 Version 1 — Modular Monolith Layout

```
atlas-platform/
├── src/main/java/com/atlas/
│   ├── config/                 (security, OpenAPI, web, JPA)
│   ├── exception/              (global handler, API error types)
│   ├── shared/                 (cross-cutting DTOs, utilities)
│   ├── modules/
│   │   ├── organizations/      (controller, service, repository, entity, dto, mapper)
│   │   ├── auth/
│   │   ├── memberships/
│   │   ├── teams/
│   │   ├── invitations/
│   │   ├── projects/
│   │   ├── tasks/
│   │   ├── milestones/
│   │   ├── labels/
│   │   ├── comments/
│   │   ├── attachments/
│   │   ├── notifications/
│   │   ├── audit/
│   │   ├── billing/
│   │   ├── activity/
│   │   └── search/
│   └── event/                  (publishers, listeners, envelope schema)
├── src/main/resources/
│   └── db/migration/           (Flyway SQL migrations)
└── docs/
```

**Module isolation rule:** a module may only import another module's
public `service` interface, never its `repository` directly — this is
the seam that makes future extraction (§10.1) a network-boundary change,
not a rewrite.

## 10.3 Technology Choices

| Concern | Choice | Rationale |
|---|---|---|
| Backend runtime | Java 17 | Production-grade ecosystem, strong typing for a permission-heavy domain |
| API framework | Spring Boot 4.x | Mature modular structure, first-class JPA/security/testing support |
| Database | PostgreSQL 16 | Relational integrity for multi-tenant, permission-heavy domain |
| ORM / migrations | Spring Data JPA + Flyway | Schema managed via versioned SQL migrations per `07-database-design.md` |
| Cache | Redis | Session store, rate-limit counters, hot-path permission cache |
| Message broker | RabbitMQ (v1) → Kafka (v3+, if Search/Analytics need replay) | Simpler ops for v1 fanout; revisit when replay/ordering guarantees matter |
| File storage | S3-compatible object storage | Attachments (doc 05 §5.12) |
| Frontend | React + Next.js | Consumes the API per doc 01 §"API-first" principle |
| Search (v3+) | Meilisearch or Elasticsearch | Decided at extraction time, not v1 |

## 10.4 Request Lifecycle (v1 monolith)

```
Client
  ↓
Load Balancer / API Gateway (TLS termination, rate limiting)
  ↓
Spring Boot App
  ├── Security Filter Chain (JWT verify → attach User)
  ├── Tenant Resolution (org from path/token → attach Organization context)
  ├── Permission Guard (role check per 08-api-contracts.md §8.9)
  ├── Controller → Service (business rules from 05-entity-specifications.md)
  ├── Repository (JPA) → PostgreSQL
  └── Event Publish (RabbitMQ) for async fanout per 09-event-flows.md
  ↓
Response
```

## 10.5 Multi-Tenancy Strategy

Shared database, shared schema, `organization_id` on every tenant-scoped
row (row-level tenancy) — not schema-per-tenant or DB-per-tenant. Chosen
for operational simplicity at v1 scale; revisit only if a specific
enterprise customer requires physical data isolation (tracked as a v4+
consideration in `13-roadmap.md`, likely solved via a dedicated deployment
rather than a schema change).

Enforcement: every JPA repository query for a tenant-scoped entity passes
through a tenant filter that injects `organization_id = ctx.currentOrgId` —
a missing tenant filter is a bug class we design out at the persistence
layer, not just catch in review.

## 10.6 Scaling Considerations (v1)

- Stateless app servers behind a load balancer — horizontal scaling is
  just adding instances.
- Postgres: read replicas once read traffic (Activity feeds, Search
  fallback queries) meaningfully exceeds write traffic.
- Redis: used for hot permission lookups to avoid a DB round-trip on
  every request; invalidated on `membership.*` events.

---
*Changelog*
- v1.0 — Monolith-first architecture with explicit extraction path defined.

# 12 — Infrastructure & Deployment

**Phase:** 6 · **Status:** Draft v1 · **Depends on:** 10-architecture.md, 11-security.md

---

## 12.1 Environments

| Env | Purpose | Deploy trigger |
|---|---|---|
| `local` | Docker Compose, seeded data | manual |
| `preview` | Ephemeral, per-PR | auto on PR open (see `GIT_WORKFLOW.md`) |
| `staging` | Pre-prod, mirrors prod config | auto on merge to `develop` |
| `production` | Live | auto on merge to `main`, manual approval gate |

## 12.2 Local Development — Docker Compose

```yaml
services:
  api:
    build: .
    ports: ["3000:3000"]
    env_file: .env
    depends_on: [postgres, redis, rabbitmq]
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: atlas
    volumes: ["pgdata:/var/lib/postgresql/data"]
  redis:
    image: redis:7
  rabbitmq:
    image: rabbitmq:3-management
    ports: ["15672:15672"]
volumes:
  pgdata:
```

## 12.3 CI/CD Pipeline (per PR)

```
PR opened
  ↓
Lint + Typecheck
  ↓
Unit tests
  ↓
Integration tests (spins up Postgres/Redis/RabbitMQ via testcontainers)
  ↓
Tenant isolation suite (11-security.md §11.7) — required, cannot be skipped
  ↓
Build
  ↓
Deploy to ephemeral preview environment
  ↓
[Reviewer approval — see GIT_WORKFLOW.md]
  ↓
Merge → staging deploy → smoke tests → (manual gate) → production deploy
```

## 12.4 Infrastructure (AWS, target)

| Component | Service |
|---|---|
| Compute | ECS Fargate (stateless app containers) |
| Database | RDS PostgreSQL (Multi-AZ in production) |
| Cache | ElastiCache Redis |
| Message broker | Amazon MQ (RabbitMQ) v1; migrate to MSK (Kafka) at extraction per `10-architecture.md` |
| Object storage | S3 (attachments), lifecycle rules for `INFECTED` quarantine purge |
| CDN | CloudFront (static frontend assets) |
| Secrets | AWS Secrets Manager |
| DNS/TLS | Route53 + ACM |

## 12.5 Monitoring & Observability

- **Metrics:** Prometheus-compatible app metrics (request latency, error
  rate, event consumer lag/DLQ depth per `09-event-flows.md` §9.7).
- **Logs:** Structured JSON logs, centralized (e.g. CloudWatch/ELK), PII
  redacted per `11-security.md` §11.4.
- **Tracing:** OpenTelemetry across API → DB → event consumers, to debug
  the async fanout paths in doc 09.
- **Alerting:** PagerDuty/Opsgenie on error-rate spikes, DLQ depth
  thresholds, and `Subscription` webhook failures.

## 12.6 Backup & Disaster Recovery

- RDS automated daily snapshots + point-in-time recovery (35-day window).
- Quarterly restore drill into a scratch environment, verified against a
  checklist (data integrity, referential checks per `07-database-design.md`).
- RPO target: 15 minutes (via PITR). RTO target: 1 hour for a full
  region-level restore.

## 12.7 Release Strategy

- Trunk-based with short-lived feature branches (see `GIT_WORKFLOW.md`).
- Database migrations are backward-compatible by convention (expand/
  contract pattern): add new columns nullable first, backfill, then
  enforce constraints in a follow-up migration — never a single PR that
  both adds a NOT NULL column and deploys code depending on it
  simultaneously.
- Feature flags for anything user-facing that's incomplete, so `main` is
  always deployable.

---
*Changelog*
- v1.0 — Environments, CI/CD, AWS target infra, and DR targets defined.

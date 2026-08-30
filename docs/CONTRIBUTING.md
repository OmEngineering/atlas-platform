# Contributing to Atlas

Read this before your first PR. It's short on purpose — the real depth
lives in `docs/`.

## Before You Write Code

1. Find (or write) the doc section that justifies what you're building.
   - New entity/field → `docs/05-entity-specifications.md`
   - New endpoint → `docs/08-api-contracts.md`
   - New async behavior → `docs/09-event-flows.md`
   - If it's not in the docs and it's more than a trivial fix, write the
     doc change first, get it reviewed, **then** build.
2. Check `docs/13-roadmap.md` — if what you're building is explicitly
   listed as out-of-scope for the current version, raise it before
   building it.

## Local Setup

```bash
git clone https://github.com/<org>/atlas-platform.git
cd atlas-platform
cp .env.example .env
docker compose up -d postgres
mvn spring-boot:run
```

Integration tests use Testcontainers and require Docker.

## Workflow

See `GIT_WORKFLOW.md` for full detail. Short version:

1. Branch off `develop`: `git checkout -b feature/your-thing`
2. Commit using Conventional Commits (`feat(tasks): ...`)
3. Open a PR against `develop` using the PR template
4. Make sure required checks pass (lint, tests, tenant-isolation suite)
5. Get review, squash-merge

## Code Standards

- Java 17, Spring Boot 4.x, constructor injection, no entity exposure in APIs.
- Every new endpoint needs: input validation, a permission guard check
  against `docs/08-api-contracts.md` §8.9, and a test covering both the
  happy path and a cross-tenant access attempt (§11.7 in `docs/11-security.md`).
- Every new mutating endpoint that touches a sensitive entity (Org,
  Membership, Billing, Auth) must write an `AuditLog` row synchronously —
  see `docs/09-event-flows.md` §9.5.
- No business logic in controllers — controllers validate + delegate to
  a service; services own the rules from `docs/05-entity-specifications.md`.

## Tests

- Unit tests for service-layer business rules.
- Integration tests for API endpoints (spun up against real Postgres/Redis
  via testcontainers, not mocks, for anything touching permissions).
- The tenant-isolation suite is mandatory and cannot be skipped — see
  `docs/11-security.md` §11.7.

## Docs Discipline

- Schema changes → update `docs/05-entity-specifications.md` +
  `docs/07-database-design.md` in the same PR.
- New/changed endpoints → update `docs/08-api-contracts.md` in the same PR.
- Architecture-affecting changes → include an ADR in the PR description
  (`GIT_WORKFLOW.md` §8).

## Getting Help

Open a draft PR early if you're unsure of direction — "here's my
approach, does this match the docs?" is a completely normal PR
description on day one of a feature.

# 11 — Security

**Phase:** 6 · **Status:** Draft v1 · **Depends on:** 10-architecture.md

---

## 11.1 Authentication

- Password auth: bcrypt/argon2 hashing, min 10 chars, breach-list check
  (e.g. HaveIBeenPwned range API) at registration/reset.
- Session: short-lived JWT access token (15 min) + rotating refresh token
  (httpOnly, secure, sameSite=strict cookie), refresh tokens revocable
  per-session (stored server-side, not purely stateless).
- Email verification required before org creation/joining (per
  `05-entity-specifications.md` §5.2).
- SSO (SAML/OIDC) — noted as v2+ in `13-roadmap.md`, entity model already
  supports `passwordHash` nullable to avoid a later migration.

## 11.2 Authorization

- Role checks (§8.9 permission matrix in `08-api-contracts.md`) are
  enforced at the API layer via a guard, **never** trusted from client
  input.
- Org boundary check runs before role check on every request: a valid
  role in Org A never grants access to a resource in Org B, checked via
  the tenant-resolution middleware (`10-architecture.md` §10.4).
- Guest role (§2.2 in `02-personas.md`) is denylist-first: guests see
  only what's explicitly whitelisted, not "everything except X."

## 11.3 Audit Trail Integrity

- `audit_logs` table has no `UPDATE`/`DELETE` grant for the application's
  DB role — enforced at the Postgres role level, not just app logic (see
  `07-database-design.md` §7.2).
- Audit writes are synchronous within the mutating transaction (see
  `09-event-flows.md` §9.5) — an audit-log write failure fails the whole
  request rather than silently dropping the record.
- Super Admin impersonation sessions are themselves audited with
  `actorType = SUPER_ADMIN` and the impersonated user's id in metadata.

## 11.4 Data Protection

- All traffic TLS 1.2+; HSTS enabled.
- PII fields (`email`, `fullName`) — no plaintext logging; structured
  logs redact known PII field names at the logging middleware level.
- Secrets (DB creds, provider API keys) via environment/secrets manager,
  never committed — enforced by a pre-commit hook + CI secret scan (see
  `GIT_WORKFLOW.md`).
- Backups: encrypted at rest, tested restore quarterly (ops runbook,
  tracked in `12-infrastructure-deployment.md`).

## 11.5 Upload Scanning

- Every `Attachment` starts `scanStatus = PENDING`.
- Async virus/malware scan (e.g. ClamAV worker) consumes an
  `attachment.uploaded` event; sets `CLEAN` or `INFECTED`.
- `INFECTED` files are never downloadable and are hard-deleted from
  storage after quarantine review (see `05-entity-specifications.md`
  §5.12).
- File type allowlist enforced at upload time (reject executables by
  default).

## 11.6 Rate Limiting & Abuse Prevention

- Per-IP and per-user rate limits on auth endpoints (login, registration,
  password reset) — sliding window via Redis.
- Per-org rate limits on invitation creation (prevent invite-spam abuse).
- Webhook endpoints (`/webhooks/stripe`) verify provider signatures;
  reject unsigned/invalid-signature requests before any processing.

## 11.7 Tenant Isolation Testing

- A dedicated test suite asserts: for every tenant-scoped model, a
  request authenticated as a member of Org A can never read/write a row
  belonging to Org B, regardless of guessed/enumerated IDs.
- This suite runs on every PR that touches `modules/*/repository` or
  Prisma middleware (see `GIT_WORKFLOW.md` §Required Checks).

## 11.8 Dependency & Supply Chain

- Automated dependency vulnerability scanning (`npm audit` / Dependabot)
  in CI; high/critical findings block merge.
- Lockfile committed, CI installs with `--frozen-lockfile` (no silent
  version drift).

## 11.9 Incident Response (skeleton — expand in ops runbook)

1. Detect (monitoring alert or report).
2. Contain (revoke sessions/tokens, disable affected endpoint if needed).
3. Assess scope using `audit_logs` (this is the primary reason audit
   integrity in §11.3 is non-negotiable).
4. Notify affected orgs per data-processing agreement timelines.
5. Post-incident review, written up, linked from `13-roadmap.md`
   §Security Backlog if it produces follow-up work.

---
*Changelog*
- v1.0 — Auth, authz, audit integrity, upload scanning, and incident skeleton defined.

# 07 — Database Design

**Phase:** 4 · **Status:** Draft v1 · **Depends on:** 05-entity-specifications.md, 06-user-journeys.md

Target: PostgreSQL 16. ORM: Spring Data JPA. Migrations: Flyway (per `10-architecture.md`).

---

## 7.1 Schema Overview

Table naming: `snake_case`, plural (`organizations`, `task_assignees`).
Every table has `id uuid default gen_random_uuid()` PK unless noted.

```
organizations
users
memberships
teams
team_members
invitations
projects
project_memberships
tasks
task_assignees
task_labels
labels
milestones
comments
attachments
audit_logs
subscriptions
notifications
notification_preferences
```

## 7.2 Core Tables (DDL sketch)

```sql
CREATE TABLE organizations (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name varchar(255) NOT NULL,
  slug varchar(100) NOT NULL UNIQUE,
  logo_url text,
  description text,
  timezone varchar(64) NOT NULL DEFAULT 'UTC',
  country char(2),
  subscription_id uuid UNIQUE REFERENCES subscriptions(id),
  status varchar(20) NOT NULL DEFAULT 'ACTIVE',
  created_by uuid NOT NULL REFERENCES users(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz
);
CREATE INDEX idx_orgs_status ON organizations(status);

CREATE TABLE memberships (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  organization_id uuid NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  role varchar(20) NOT NULL,
  invited_by uuid REFERENCES users(id),
  joined_at timestamptz NOT NULL DEFAULT now(),
  status varchar(20) NOT NULL DEFAULT 'ACTIVE',
  UNIQUE(organization_id, user_id)
);
CREATE INDEX idx_memberships_user ON memberships(user_id);

-- Enforce "exactly one ACTIVE OWNER per org" via partial unique index
CREATE UNIQUE INDEX uq_one_owner_per_org
  ON memberships(organization_id)
  WHERE role = 'OWNER' AND status = 'ACTIVE';

CREATE TABLE tasks (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id uuid NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  milestone_id uuid REFERENCES milestones(id) ON DELETE SET NULL,
  key varchar(20) NOT NULL,
  title varchar(500) NOT NULL,
  description text,
  status varchar(20) NOT NULL DEFAULT 'TODO',
  priority varchar(10) NOT NULL DEFAULT 'MEDIUM',
  due_date date,
  completed_at timestamptz,
  created_by uuid NOT NULL REFERENCES users(id),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  UNIQUE(project_id, key)
);
CREATE INDEX idx_tasks_project_status ON tasks(project_id, status);
CREATE INDEX idx_tasks_milestone ON tasks(milestone_id);

CREATE TABLE audit_logs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  organization_id uuid REFERENCES organizations(id),
  actor_id uuid REFERENCES users(id),
  actor_type varchar(20) NOT NULL,
  action varchar(100) NOT NULL,
  target_type varchar(50) NOT NULL,
  target_id uuid NOT NULL,
  metadata jsonb NOT NULL DEFAULT '{}',
  ip_address inet,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_org_created ON audit_logs(organization_id, created_at DESC);
CREATE INDEX idx_audit_target ON audit_logs(target_type, target_id);
-- No UPDATE/DELETE grant on this table for the app DB role (see 11-security.md)
```

*(Remaining tables follow the same pattern per `05-entity-specifications.md`
— full DDL lives in `src/main/resources/db/migration/` Flyway scripts, not
duplicated here to avoid drift. This doc defines intent; the migration files
are the literal source of truth.)*

## 7.3 Foreign Key & Cascade Policy

| Relationship | On delete |
|---|---|
| Organization → Membership | CASCADE (org delete removes memberships) — but org delete is itself blocked by business rules, see 05.1 |
| Organization → Project | RESTRICT at DB, enforced as soft-archive at app layer |
| Project → Task | CASCADE (soft, via `deleted_at`, not hard FK cascade in practice — app sets `deleted_at` rather than issuing DELETE) |
| Task → Comment/Attachment | CASCADE |
| Milestone → Task | SET NULL (`milestone_id` nulled, task survives) |
| User → * | RESTRICT everywhere; users are deactivated, not deleted |

## 7.4 Polymorphic Reference Strategy

`audit_logs`, `notifications`, `activity_entries`, `search_index_entries`
use `(target_type, target_id)` instead of per-type foreign keys.

**Alternative considered and rejected:** a nullable FK column per
possible target type (`task_id`, `comment_id`, `project_id`, ...) on each
of these four tables. Rejected because it means a schema migration every
time a new auditable/notifiable entity is added, and most columns would
be null on any given row. The chosen approach trades DB-level referential
integrity for schema stability; validity of `target_id` is enforced in
the service layer and covered by tests.

## 7.5 Indexing Principles

- Every FK column gets an index unless it's the leading column of a
  composite unique index that already covers it.
- Every table with an `organization_id` (directly or transitively via
  `project_id`) that's queried by org in hot paths gets a composite index
  with `organization_id`/`project_id` leading — multi-tenant queries are
  the majority of read traffic.
- `audit_logs` and future high-volume append-only tables are candidates
  for **partitioning by month** once volume warrants it (see §7.7).

## 7.6 Soft Delete Convention

Entities with a lifecycle worth preserving (`organizations`, `projects`,
`tasks`, `comments`, `users`) use `deleted_at timestamptz NULL`. All
default Prisma queries apply a `deleted_at IS NULL` filter via a
middleware; hard deletion is a separate, explicitly-named operation
(`purgeTask`, admin-only, used by scheduled cleanup jobs after the
30-day trash window referenced in `04-domain-model-relationships.md`).

## 7.7 Partitioning (deferred, noted for later)

Not needed at v1 scale. When `audit_logs` exceeds ~50M rows or query
latency degrades, partition by `created_at` (monthly range partitions),
matching the access pattern (recent-first, time-bounded queries). Tracked
in `13-roadmap.md`.

## 7.8 Migration Discipline

- One migration file per PR that touches schema; named
  `V{version}__description.sql` (Flyway naming convention).
- No migration is squashed/rewritten after merge to `main` — forward-only.
- Every migration PR must update the relevant section of
  `05-entity-specifications.md` in the same PR (see `GIT_WORKFLOW.md`
  §Schema-Change PRs).

---
*Changelog*
- v1.0 — Core DDL sketched, cascade/indexing/partitioning policy set.

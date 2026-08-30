# Git Workflow

This is how Atlas ships changes. It applies to code **and** to the
`docs/` folder — a docs-only change goes through the same PR process,
just with a lighter review bar.

---

## 1. Branching Model

Trunk-based, two long-lived branches:

- `main` — always deployable, protected, production deploys from here.
- `develop` — integration branch, staging deploys from here.

Everything else is a short-lived branch off `develop`, merged back via PR,
and deleted after merge.

```
main ────────────────────●───────────●──────────►  (production)
                          ▲           ▲
develop ──●───●───●───●───●───●───●───●──────────►  (staging)
           ▲       ▲           ▲
feature/*  │       │           │
fix/*      └───────┘           │
docs/*                         └── release branch (cut from develop → main)
```

## 2. Branch Naming

```
feature/<short-description>       feature/task-assignee-multi
fix/<short-description>           fix/membership-owner-race-condition
docs/<short-description>          docs/api-billing-endpoints
chore/<short-description>         chore/upgrade-prisma-5
refactor/<short-description>      refactor/extract-permission-guard
release/<version>                 release/v1.2.0
hotfix/<short-description>        hotfix/webhook-signature-check
```

Rules:
- All lowercase, hyphen-separated, no ticket-ID-only names (`feature/ATL-142`
  alone isn't descriptive — use `feature/ATL-142-task-multi-assignee`).
- Branch off `develop`, except `hotfix/*` which branches off `main` and
  merges to **both** `main` and `develop`.

## 3. Commit Convention (Conventional Commits)

```
<type>(<scope>): <short summary>

[optional body]

[optional footer: BREAKING CHANGE: ..., Refs: ATL-142]
```

**Types:** `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `perf`, `ci`

**Scope** = the module from `docs/03-business-domains-and-modules.md`
(`tasks`, `billing`, `auth`, `docs`, etc.)

Examples:
```
feat(tasks): support multiple assignees per task
fix(memberships): prevent second OWNER row via race condition
docs(api): document billing checkout endpoint
refactor(auth): extract permission guard into shared module
chore(deps): bump prisma to 5.18
```

- One logical change per commit. Squash-merge is used at the PR level
  (see §5), so commit hygiene on the branch matters less than PR-title
  hygiene — but write clean commits anyway, they're the diff reviewers read.

## 4. Opening a PR

Every PR:
1. Targets `develop` (never `main` directly, except `hotfix/*`).
2. Uses the PR template (`.github/PULL_REQUEST_TEMPLATE.md`).
3. Has a title in Conventional Commit format — this becomes the squash
   commit message.
4. Links the relevant doc section(s) it implements or changes.
5. Is small enough to review in one sitting. If a feature genuinely needs
   >500 lines of diff, split it into stacked PRs (schema migration →
   service logic → API endpoint → frontend) rather than one giant PR.

## 5. Merge Strategy

- **Squash merge** into `develop` — one clean commit per PR on the
  integration branch.
- `develop` → `main` via a **release PR** (no squash — merge commit,
  preserving the release's commit history), cut on a cadence or when a
  meaningful batch of work is staging-verified.
- `hotfix/*` → `main` via fast-tracked PR (still reviewed, expedited),
  then cherry-picked/merged into `develop` immediately after.

## 6. Required Checks (block merge if failing)

- Lint + typecheck
- Unit + integration tests
- Tenant isolation suite (`docs/11-security.md` §11.7) — **non-negotiable**,
  cannot be skipped even for urgent PRs
- Dependency vulnerability scan (high/critical blocks)
- At least 1 approving review; 2 for anything touching `modules/*/repository`,
  `prisma/schema.prisma`, or `docs/05-entity-specifications.md` /
  `docs/07-database-design.md`

## 7. Schema-Change PRs (special rule)

If a PR modifies `prisma/schema.prisma` (or equivalent), it **must** in
the same PR:
1. Include the migration file.
2. Update the relevant entity in `docs/05-entity-specifications.md`.
3. Update `docs/07-database-design.md` if indexes/cascade rules change.
4. Follow the expand/contract pattern from `docs/12-infrastructure-deployment.md`
   §12.7 — no single PR both adds a NOT NULL constraint and ships code
   that depends on it existing from day one.

A PR that changes schema without touching docs gets an automatic
"docs out of sync" review comment and should not be approved.

## 8. Architecture-Change PRs (ADR requirement)

Any PR that changes something in `docs/10-architecture.md` (new tech
choice, service extraction, changed data flow) must link a short ADR
(Architecture Decision Record) in the PR description:

```markdown
## ADR: <title>
**Context:** what problem are we solving
**Decision:** what we're doing
**Alternatives considered:** what else we looked at, why rejected
**Consequences:** what this makes easier/harder going forward
```

Keep it to half a page. This is a decision log, not a design doc.

## 9. Documentation-Only PRs

Docs-only changes (typo fixes, roadmap updates, clarifications) still go
through a PR, but only need 1 reviewer and skip the schema/ADR
requirements above. Use `docs/*` branch prefix so this is obvious in the
PR list.

## 10. Release Process

```
1. Cut `release/vX.Y.Z` from `develop`
2. Deploy to staging, run full regression + manual QA pass
3. Bump version, update docs/13-roadmap.md if scope shifted
4. Open PR: release/vX.Y.Z → main
5. Merge (merge commit, not squash) → triggers production deploy
   (manual approval gate per docs/12-infrastructure-deployment.md §12.1)
6. Tag the release: vX.Y.Z
7. Fast-forward merge main back into develop (keeps them in sync)
```

## 11. Emergency Hotfix Process

```
1. Branch hotfix/<desc> from main
2. Fix, minimal diff, tests included
3. Fast-tracked PR review (still required, just expedited)
4. Merge to main → deploy
5. Merge/cherry-pick same commit into develop immediately
6. Add a line to docs/13-roadmap.md if it reveals a deferred risk worth tracking
```

---
*Changelog*
- v1.0 — Branching model, commit convention, PR rules, and release process defined.

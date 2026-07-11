-- Atlas v1 baseline schema per docs/05-entity-specifications.md and docs/07-database-design.md

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255),
    full_name       VARCHAR(255) NOT NULL,
    avatar_url      TEXT,
    email_verified_at TIMESTAMPTZ,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'DEACTIVATED'))
);

CREATE INDEX idx_users_status ON users (status);

CREATE TABLE organizations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(100) NOT NULL,
    logo_url        TEXT,
    description     TEXT,
    timezone        VARCHAR(64) NOT NULL DEFAULT 'UTC',
    country         CHAR(2),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by      UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT uq_organizations_slug UNIQUE (slug),
    CONSTRAINT chk_organizations_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'ARCHIVED'))
);

CREATE INDEX idx_organizations_status ON organizations (status);
CREATE INDEX idx_organizations_created_by ON organizations (created_by);

CREATE TABLE subscriptions (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id           UUID NOT NULL REFERENCES organizations (id) ON DELETE RESTRICT,
    plan                      VARCHAR(20) NOT NULL DEFAULT 'FREE',
    seats                     INTEGER NOT NULL DEFAULT 1,
    status                    VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    provider_customer_id      VARCHAR(255),
    provider_subscription_id  VARCHAR(255),
    current_period_end        TIMESTAMPTZ,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_subscriptions_organization_id UNIQUE (organization_id),
    CONSTRAINT chk_subscriptions_plan CHECK (plan IN ('FREE', 'TEAM', 'BUSINESS', 'ENTERPRISE')),
    CONSTRAINT chk_subscriptions_status CHECK (status IN ('ACTIVE', 'PAST_DUE', 'CANCELLED'))
);

CREATE INDEX idx_subscriptions_status ON subscriptions (status);

ALTER TABLE organizations
    ADD COLUMN subscription_id UUID UNIQUE REFERENCES subscriptions (id) ON DELETE RESTRICT;

CREATE TABLE memberships (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    role            VARCHAR(20) NOT NULL,
    invited_by      UUID REFERENCES users (id) ON DELETE SET NULL,
    joined_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT uq_memberships_org_user UNIQUE (organization_id, user_id),
    CONSTRAINT chk_memberships_role CHECK (role IN ('OWNER', 'ADMIN', 'BILLING_MANAGER', 'MEMBER')),
    CONSTRAINT chk_memberships_status CHECK (status IN ('ACTIVE', 'REMOVED'))
);

CREATE INDEX idx_memberships_user ON memberships (user_id);

CREATE UNIQUE INDEX uq_one_owner_per_org
    ON memberships (organization_id)
    WHERE role = 'OWNER' AND status = 'ACTIVE';

CREATE TABLE teams (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    created_by      UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_teams_org_name UNIQUE (organization_id, name)
);

CREATE TABLE team_members (
    id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id  UUID NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    user_id  UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    added_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_team_members_team_user UNIQUE (team_id, user_id)
);

CREATE TABLE invitations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    project_id      UUID,
    email           VARCHAR(255) NOT NULL,
    role            VARCHAR(20) NOT NULL,
    token           VARCHAR(255) NOT NULL,
    invited_by      UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_invitations_token UNIQUE (token),
    CONSTRAINT chk_invitations_status CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED'))
);

CREATE INDEX idx_invitations_org_email ON invitations (organization_id, email);

CREATE TABLE projects (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE RESTRICT,
    name            VARCHAR(255) NOT NULL,
    key             VARCHAR(20) NOT NULL,
    description     TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    visibility      VARCHAR(20) NOT NULL DEFAULT 'ORG_WIDE',
    created_by      UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT uq_projects_org_key UNIQUE (organization_id, key),
    CONSTRAINT chk_projects_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT chk_projects_visibility CHECK (visibility IN ('ORG_WIDE', 'RESTRICTED'))
);

CREATE INDEX idx_projects_org_status ON projects (organization_id, status);

ALTER TABLE invitations
    ADD CONSTRAINT fk_invitations_project
        FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE;

CREATE TABLE project_memberships (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    role       VARCHAR(20) NOT NULL,
    added_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    added_by   UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT uq_project_memberships_project_user UNIQUE (project_id, user_id),
    CONSTRAINT chk_project_memberships_role CHECK (role IN ('PM', 'MEMBER', 'GUEST'))
);

CREATE TABLE milestones (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    start_date  DATE,
    due_date    DATE,
    status      VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_milestones_status CHECK (status IN ('PLANNED', 'ACTIVE', 'COMPLETED'))
);

CREATE INDEX idx_milestones_project_status ON milestones (project_id, status);

CREATE TABLE labels (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    project_id      UUID REFERENCES projects (id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    color           VARCHAR(7) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_labels_org_project_name UNIQUE (organization_id, project_id, name)
);

CREATE TABLE tasks (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id   UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    milestone_id UUID REFERENCES milestones (id) ON DELETE SET NULL,
    key          VARCHAR(20) NOT NULL,
    title        VARCHAR(500) NOT NULL,
    description  TEXT,
    status       VARCHAR(20) NOT NULL DEFAULT 'TODO',
    priority     VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    due_date     DATE,
    completed_at TIMESTAMPTZ,
    created_by   UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at   TIMESTAMPTZ,
    CONSTRAINT uq_tasks_project_key UNIQUE (project_id, key),
    CONSTRAINT chk_tasks_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE', 'CANCELLED')),
    CONSTRAINT chk_tasks_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT'))
);

CREATE INDEX idx_tasks_project_status ON tasks (project_id, status);
CREATE INDEX idx_tasks_milestone ON tasks (milestone_id);

CREATE TABLE task_assignees (
    task_id UUID NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    PRIMARY KEY (task_id, user_id)
);

CREATE TABLE task_labels (
    task_id  UUID NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    label_id UUID NOT NULL REFERENCES labels (id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, label_id)
);

CREATE TABLE comments (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id    UUID NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    author_id  UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    body       TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    edited_at  TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_comments_task_created_at ON comments (task_id, created_at);

CREATE TABLE attachments (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id      UUID REFERENCES tasks (id) ON DELETE CASCADE,
    comment_id   UUID REFERENCES comments (id) ON DELETE CASCADE,
    uploaded_by  UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    file_name    VARCHAR(255) NOT NULL,
    mime_type    VARCHAR(127) NOT NULL,
    size_bytes   BIGINT NOT NULL,
    storage_key  VARCHAR(512) NOT NULL,
    scan_status  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_attachments_scan_status CHECK (scan_status IN ('PENDING', 'CLEAN', 'INFECTED')),
    CONSTRAINT chk_attachments_parent CHECK (
        (task_id IS NOT NULL AND comment_id IS NULL)
        OR (task_id IS NULL AND comment_id IS NOT NULL)
    )
);

CREATE INDEX idx_attachments_task ON attachments (task_id);
CREATE INDEX idx_attachments_comment ON attachments (comment_id);

CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID REFERENCES organizations (id) ON DELETE RESTRICT,
    actor_id        UUID REFERENCES users (id) ON DELETE SET NULL,
    actor_type      VARCHAR(20) NOT NULL,
    action          VARCHAR(100) NOT NULL,
    target_type     VARCHAR(50) NOT NULL,
    target_id       UUID NOT NULL,
    metadata        JSONB NOT NULL DEFAULT '{}',
    ip_address      INET,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_audit_logs_actor_type CHECK (actor_type IN ('USER', 'SYSTEM', 'SUPER_ADMIN'))
);

CREATE INDEX idx_audit_logs_org_created_at ON audit_logs (organization_id, created_at DESC);
CREATE INDEX idx_audit_logs_target ON audit_logs (target_type, target_id);

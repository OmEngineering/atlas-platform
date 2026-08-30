-- Activity timeline (human-readable feed from domain events)

CREATE TABLE activity_entries (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    project_id       UUID REFERENCES projects (id) ON DELETE SET NULL,
    actor_id         UUID REFERENCES users (id) ON DELETE SET NULL,
    event_type       VARCHAR(100) NOT NULL,
    summary          TEXT NOT NULL,
    target_type      VARCHAR(50) NOT NULL,
    target_id        UUID NOT NULL,
    metadata         JSONB NOT NULL DEFAULT '{}',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_activity_entries_target_type
        CHECK (target_type IN (
            'ORGANIZATION', 'PROJECT', 'TASK', 'COMMENT', 'MEMBERSHIP',
            'INVITATION', 'SUBSCRIPTION', 'TEAM', 'MILESTONE', 'ATTACHMENT'
        ))
);

CREATE INDEX idx_activity_entries_org_created
    ON activity_entries (organization_id, created_at DESC, id DESC);

CREATE INDEX idx_activity_entries_org_project
    ON activity_entries (organization_id, project_id, created_at DESC)
    WHERE project_id IS NOT NULL;

CREATE INDEX idx_activity_entries_target
    ON activity_entries (target_type, target_id);

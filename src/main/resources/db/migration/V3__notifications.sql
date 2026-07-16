-- Notifications module per docs/05-entity-specifications.md §5.15–5.16

CREATE TABLE notifications (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    organization_id  UUID REFERENCES organizations (id) ON DELETE SET NULL,
    event_type       VARCHAR(100) NOT NULL,
    title            VARCHAR(255) NOT NULL,
    body             TEXT,
    target_type      VARCHAR(50) NOT NULL,
    target_id        UUID NOT NULL,
    read_at          TIMESTAMPTZ,
    email_status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    email_sent_at    TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_notifications_email_status
        CHECK (email_status IN ('PENDING', 'SENT', 'SKIPPED', 'FAILED')),
    CONSTRAINT chk_notifications_target_type
        CHECK (target_type IN (
            'ORGANIZATION', 'PROJECT', 'TASK', 'COMMENT', 'MEMBERSHIP',
            'INVITATION', 'SUBSCRIPTION', 'TEAM', 'MILESTONE', 'ATTACHMENT'
        ))
);

CREATE INDEX idx_notifications_user_created
    ON notifications (user_id, created_at DESC);

CREATE INDEX idx_notifications_user_unread
    ON notifications (user_id)
    WHERE read_at IS NULL;

CREATE TABLE notification_preferences (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    in_app_enabled  BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_notification_preferences_user UNIQUE (user_id)
);

CREATE TABLE IF NOT EXISTS notifications (
    id              UUID                NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID                NOT NULL,
    type            notification_type   NOT NULL,
    title           VARCHAR(255)        NOT NULL,
    body            TEXT                NOT NULL,
    reference_id    UUID                NULL,
    reference_type  VARCHAR(100)        NULL,
    is_read         BOOLEAN             NOT NULL DEFAULT FALSE,
    read_at         TIMESTAMPTZ         NULL,
    created_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_notifications_reference_consistency CHECK (
        (reference_id IS NULL) = (reference_type IS NULL)
    ),
    CONSTRAINT chk_notifications_read_at CHECK (
        (is_read = FALSE AND read_at IS NULL)
        OR (is_read = TRUE  AND read_at IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id_unread    ON notifications (user_id, created_at DESC) WHERE is_read = FALSE;
CREATE INDEX IF NOT EXISTS idx_notifications_user_id_created_at ON notifications (user_id, created_at DESC);

COMMENT ON TABLE  notifications                IS 'In-app notification inbox. Append-only; no hard deletes. Marked as read, never deleted.';
COMMENT ON COLUMN notifications.reference_id   IS 'UUID of the entity that triggered this notification (polymorphic reference).';
COMMENT ON COLUMN notifications.reference_type IS 'Type discriminator for reference_id. Values: APPLICATION, INTERVIEW, JOB, USER.';

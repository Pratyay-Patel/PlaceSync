CREATE TABLE IF NOT EXISTS audit_log (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    entity_type     VARCHAR(100)    NOT NULL,
    entity_id       UUID            NOT NULL,
    action          audit_action    NOT NULL,
    actor_id        UUID            NULL,
    actor_role      VARCHAR(50)     NULL,
    actor_email     VARCHAR(255)    NULL,
    old_values      JSONB           NULL,
    new_values      JSONB           NULL,
    ip_address      INET            NULL,
    user_agent      VARCHAR(500)    NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_audit_log PRIMARY KEY (id)
    -- Intentionally no FK on actor_id: audit records must outlive user records.
);

CREATE INDEX IF NOT EXISTS idx_audit_log_entity_type_created_at ON audit_log (entity_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_actor_id_created_at    ON audit_log (actor_id,    created_at DESC) WHERE actor_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at             ON audit_log (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_entity_id              ON audit_log (entity_id);

COMMENT ON TABLE  audit_log            IS 'Immutable, append-only audit trail. No UPDATE or DELETE permitted. actor_role and actor_email are intentionally denormalized.';
COMMENT ON COLUMN audit_log.actor_id   IS 'user_id of the actor. NULL for system actions. No FK constraint: audit records outlive user records.';
COMMENT ON COLUMN audit_log.old_values IS 'JSONB snapshot of relevant fields before the change. NULL for CREATE actions.';
COMMENT ON COLUMN audit_log.new_values IS 'JSONB snapshot of relevant fields after the change. NULL for DELETE actions.';
COMMENT ON COLUMN audit_log.ip_address IS 'Uses PostgreSQL INET type which validates IP format and supports CIDR range queries.';

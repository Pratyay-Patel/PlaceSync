CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id     UUID            NOT NULL,
    token_hash  VARCHAR(255)    NOT NULL,
    family_id   UUID            NOT NULL,
    expires_at  TIMESTAMPTZ     NOT NULL,
    is_revoked  BOOLEAN         NOT NULL DEFAULT FALSE,
    revoked_at  TIMESTAMPTZ     NULL,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT chk_refresh_tokens_revoked_at CHECK (
        (is_revoked = FALSE AND revoked_at IS NULL)
        OR (is_revoked = TRUE  AND revoked_at IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id   ON refresh_tokens (user_id)    WHERE is_revoked = FALSE;
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_family_id ON refresh_tokens (family_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens (expires_at) WHERE is_revoked = FALSE;

COMMENT ON TABLE  refresh_tokens            IS 'JWT refresh token store. Hashed values only. family_id enables reuse detection and session-family invalidation.';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'SHA-256 hash of the raw opaque refresh token. The raw value is only held by the client.';
COMMENT ON COLUMN refresh_tokens.family_id  IS 'UUID grouping all tokens from the same login session. Invalidating by family_id logs out all sessions sharing that origin.';

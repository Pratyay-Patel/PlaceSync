CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id          UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id     UUID            NOT NULL,
    token_hash  VARCHAR(255)    NOT NULL,
    expires_at  TIMESTAMPTZ     NOT NULL,
    is_used     BOOLEAN         NOT NULL DEFAULT FALSE,
    used_at     TIMESTAMPTZ     NULL,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_password_reset_tokens_hash UNIQUE (token_hash),
    CONSTRAINT chk_prt_used_at CHECK (
        (is_used = FALSE AND used_at IS NULL)
        OR (is_used = TRUE  AND used_at IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_prt_expires_at ON password_reset_tokens (expires_at) WHERE is_used = FALSE;

COMMENT ON TABLE password_reset_tokens IS 'Single-use, 1-hour tokens for password reset. Hashed values only. Separate from email verification tokens for domain clarity.';

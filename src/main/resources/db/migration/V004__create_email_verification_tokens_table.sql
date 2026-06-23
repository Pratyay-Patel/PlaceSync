CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id          UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id     UUID            NOT NULL,
    token_hash  VARCHAR(255)    NOT NULL,
    expires_at  TIMESTAMPTZ     NOT NULL,
    is_used     BOOLEAN         NOT NULL DEFAULT FALSE,
    used_at     TIMESTAMPTZ     NULL,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_email_verification_tokens PRIMARY KEY (id),
    CONSTRAINT fk_evtoken_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_email_verification_tokens_hash UNIQUE (token_hash),
    CONSTRAINT chk_evtoken_used_at CHECK (
        (is_used = FALSE AND used_at IS NULL)
        OR (is_used = TRUE  AND used_at IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_evtoken_expires_at ON email_verification_tokens (expires_at) WHERE is_used = FALSE;

COMMENT ON TABLE email_verification_tokens IS 'Single-use, time-limited tokens for email address verification. Hashed values only.';

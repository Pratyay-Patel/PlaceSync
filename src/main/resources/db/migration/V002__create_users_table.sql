CREATE TABLE IF NOT EXISTS users (
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    email                   VARCHAR(255)    NOT NULL,
    password_hash           VARCHAR(255)    NULL,
    role                    user_role       NOT NULL,
    is_email_verified       BOOLEAN         NOT NULL DEFAULT FALSE,
    is_active               BOOLEAN         NOT NULL DEFAULT TRUE,
    failed_login_attempts   SMALLINT        NOT NULL DEFAULT 0,
    locked_until            TIMESTAMPTZ     NULL,
    oauth_provider          VARCHAR(50)     NULL,
    oauth_provider_id       VARCHAR(255)    NULL,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at              TIMESTAMPTZ     NULL,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_oauth UNIQUE (oauth_provider, oauth_provider_id),
    CONSTRAINT chk_users_password_or_oauth CHECK (
        (password_hash IS NOT NULL AND oauth_provider IS NULL)
        OR (password_hash IS NULL  AND oauth_provider IS NOT NULL)
        OR (password_hash IS NOT NULL AND oauth_provider IS NOT NULL)
    ),
    CONSTRAINT chk_users_failed_attempts CHECK (failed_login_attempts >= 0),
    CONSTRAINT chk_users_oauth_consistency CHECK (
        (oauth_provider IS NULL) = (oauth_provider_id IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_users_email        ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_role_active  ON users (role, is_active) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_users_active       ON users (id)              WHERE deleted_at IS NULL;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE  users                         IS 'Central identity record for all user roles. Single source of truth for authentication.';
COMMENT ON COLUMN users.password_hash           IS 'BCrypt hash (cost 12). NULL for pure OAuth2 users.';
COMMENT ON COLUMN users.oauth_provider_id       IS 'Provider-issued user ID (e.g. Google sub claim). Never the provider access token.';
COMMENT ON COLUMN users.locked_until            IS 'Account lock expiry after repeated failed login attempts. NULL means not locked.';
COMMENT ON COLUMN users.deleted_at              IS 'Soft delete timestamp. NULL means active. Physical deletion does not occur in V1.';

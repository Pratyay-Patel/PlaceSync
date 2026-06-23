CREATE TABLE IF NOT EXISTS companies (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    name            VARCHAR(255)    NOT NULL,
    description     TEXT            NULL,
    website_url     VARCHAR(500)    NULL,
    industry        VARCHAR(255)    NULL,
    headquarters    VARCHAR(255)    NULL,
    logo_s3_key     VARCHAR(500)    NULL,
    status          company_status  NOT NULL DEFAULT 'PENDING_VERIFICATION',
    created_by      UUID            NOT NULL,
    verified_by     UUID            NULL,
    verified_at     TIMESTAMPTZ     NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ     NULL,

    CONSTRAINT pk_companies PRIMARY KEY (id),
    CONSTRAINT fk_companies_created_by  FOREIGN KEY (created_by)  REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_companies_verified_by FOREIGN KEY (verified_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT uq_companies_name UNIQUE (name),
    CONSTRAINT chk_companies_verification_consistency CHECK (
        (verified_by IS NULL AND verified_at IS NULL)
        OR (verified_by IS NOT NULL AND verified_at IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_companies_status ON companies (status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_companies_name   ON companies (name)   WHERE deleted_at IS NULL;

CREATE TRIGGER trg_companies_updated_at
    BEFORE UPDATE ON companies
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE  companies             IS 'Company profiles created by recruiters, approved by admins.';
COMMENT ON COLUMN companies.logo_s3_key IS 'AWS S3 object key for the company logo image. Accessed via pre-signed URL.';
COMMENT ON COLUMN companies.created_by  IS 'user_id of the recruiter who created this company record.';
COMMENT ON COLUMN companies.verified_by IS 'user_id of the admin who approved or rejected the company. NULL until a decision is made.';

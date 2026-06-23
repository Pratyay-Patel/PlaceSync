CREATE TABLE IF NOT EXISTS recruiter_profiles (
    id                      UUID                    NOT NULL DEFAULT gen_random_uuid(),
    user_id                 UUID                    NOT NULL,
    first_name              VARCHAR(100)            NOT NULL,
    last_name               VARCHAR(100)            NOT NULL,
    job_title               VARCHAR(255)            NULL,
    contact_email           VARCHAR(255)            NULL,
    phone                   VARCHAR(20)             NULL,
    company_id              UUID                    NULL,
    verification_status     verification_status     NOT NULL DEFAULT 'PENDING_VERIFICATION',
    verified_at             TIMESTAMPTZ             NULL,
    verified_by             UUID                    NULL,
    rejection_reason        TEXT                    NULL,
    created_at              TIMESTAMPTZ             NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ             NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_recruiter_profiles PRIMARY KEY (id),
    CONSTRAINT fk_recruiter_profiles_user        FOREIGN KEY (user_id)    REFERENCES users (id)      ON DELETE RESTRICT,
    CONSTRAINT uq_recruiter_profiles_user_id     UNIQUE (user_id),
    CONSTRAINT fk_recruiter_profiles_company     FOREIGN KEY (company_id) REFERENCES companies (id)  ON DELETE SET NULL,
    CONSTRAINT fk_recruiter_profiles_verified_by FOREIGN KEY (verified_by) REFERENCES users (id)     ON DELETE RESTRICT,
    CONSTRAINT chk_recruiter_verification_consistency CHECK (
        (verified_by IS NULL AND verified_at IS NULL)
        OR (verified_by IS NOT NULL AND verified_at IS NOT NULL)
    ),
    CONSTRAINT chk_recruiter_rejection_reason CHECK (
        verification_status != 'REJECTED' OR rejection_reason IS NOT NULL
    )
);

CREATE INDEX IF NOT EXISTS idx_recruiter_profiles_verification_status ON recruiter_profiles (verification_status);
CREATE INDEX IF NOT EXISTS idx_recruiter_profiles_company_id          ON recruiter_profiles (company_id) WHERE company_id IS NOT NULL;

CREATE TRIGGER trg_recruiter_profiles_updated_at
    BEFORE UPDATE ON recruiter_profiles
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE  recruiter_profiles                IS '1:1 extension of users for recruiters. Tracks verification status and company association.';
COMMENT ON COLUMN recruiter_profiles.contact_email  IS 'Business email for candidate-facing correspondence. May differ from the login email on users.email.';
COMMENT ON COLUMN recruiter_profiles.verified_by    IS 'user_id of the ROLE_ADMIN who made the verification decision.';

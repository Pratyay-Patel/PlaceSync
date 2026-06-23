CREATE TABLE IF NOT EXISTS jobs (
    id                      UUID                NOT NULL DEFAULT gen_random_uuid(),
    recruiter_id            UUID                NOT NULL,
    company_id              UUID                NOT NULL,
    title                   VARCHAR(255)        NOT NULL,
    description             TEXT                NOT NULL,
    location_type           job_location_type   NOT NULL,
    job_type                job_type            NOT NULL,
    location_city           VARCHAR(255)        NULL,
    compensation            VARCHAR(255)        NULL,
    application_deadline    TIMESTAMPTZ         NOT NULL,
    min_cgpa                NUMERIC(3, 2)       NULL,
    status                  job_status          NOT NULL DEFAULT 'PENDING_APPROVAL',
    approved_by             UUID                NULL,
    approved_at             TIMESTAMPTZ         NULL,
    closed_at               TIMESTAMPTZ         NULL,
    created_at              TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    deleted_at              TIMESTAMPTZ         NULL,

    CONSTRAINT pk_jobs PRIMARY KEY (id),
    CONSTRAINT fk_jobs_recruiter    FOREIGN KEY (recruiter_id) REFERENCES recruiter_profiles (id) ON DELETE RESTRICT,
    CONSTRAINT fk_jobs_company      FOREIGN KEY (company_id)   REFERENCES companies (id)          ON DELETE RESTRICT,
    CONSTRAINT fk_jobs_approved_by  FOREIGN KEY (approved_by)  REFERENCES users (id)              ON DELETE RESTRICT,
    CONSTRAINT chk_jobs_min_cgpa CHECK (min_cgpa IS NULL OR (min_cgpa >= 0.00 AND min_cgpa <= 10.00)),
    CONSTRAINT chk_jobs_approval_consistency CHECK (
        (approved_by IS NULL AND approved_at IS NULL)
        OR (approved_by IS NOT NULL AND approved_at IS NOT NULL)
    ),
    CONSTRAINT chk_jobs_location_city CHECK (
        location_type = 'REMOTE' OR location_city IS NOT NULL
    )
);

CREATE INDEX IF NOT EXISTS idx_jobs_status_deadline ON jobs (status, application_deadline) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_jobs_company_id      ON jobs (company_id)                   WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_jobs_recruiter_id    ON jobs (recruiter_id)                 WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_jobs_type_location   ON jobs (job_type, location_type)      WHERE status = 'OPEN' AND deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_jobs_deadline_open   ON jobs (application_deadline)         WHERE status = 'OPEN' AND deleted_at IS NULL;

CREATE TRIGGER trg_jobs_updated_at
    BEFORE UPDATE ON jobs
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE  jobs              IS 'Job postings created by recruiters, approved by admins, browsed by students.';
COMMENT ON COLUMN jobs.compensation IS 'Human-readable compensation string. Not a numeric to accommodate CTC and stipend formats.';
COMMENT ON COLUMN jobs.min_cgpa     IS 'Minimum CGPA for eligibility. Application-layer enforced; NULL means no CGPA filter.';
COMMENT ON COLUMN jobs.closed_at    IS 'Set when job is closed manually by recruiter or automatically when deadline passes.';

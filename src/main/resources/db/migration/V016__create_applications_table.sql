CREATE TABLE IF NOT EXISTS applications (
    id                  UUID                NOT NULL DEFAULT gen_random_uuid(),
    student_id          UUID                NOT NULL,
    job_id              UUID                NOT NULL,
    resume_id           UUID                NOT NULL,
    status              application_status  NOT NULL DEFAULT 'APPLIED',
    applied_at          TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    status_updated_at   TIMESTAMPTZ         NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_applications PRIMARY KEY (id),
    CONSTRAINT fk_applications_student FOREIGN KEY (student_id) REFERENCES student_profiles (id) ON DELETE RESTRICT,
    CONSTRAINT fk_applications_job     FOREIGN KEY (job_id)     REFERENCES jobs (id)             ON DELETE RESTRICT,
    CONSTRAINT fk_applications_resume  FOREIGN KEY (resume_id)  REFERENCES resumes (id)          ON DELETE RESTRICT,
    CONSTRAINT uq_applications_student_job UNIQUE (student_id, job_id)
);

CREATE INDEX IF NOT EXISTS idx_applications_student_id      ON applications (student_id, applied_at DESC);
CREATE INDEX IF NOT EXISTS idx_applications_job_id_status   ON applications (job_id, status);
CREATE INDEX IF NOT EXISTS idx_applications_status          ON applications (status);
CREATE INDEX IF NOT EXISTS idx_applications_status_job_id   ON applications (status, job_id) WHERE status = 'OFFERED';

CREATE TRIGGER trg_applications_updated_at
    BEFORE UPDATE ON applications
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE  applications                  IS 'Student applications to jobs. UNIQUE(student_id, job_id) prevents duplicate applications.';
COMMENT ON COLUMN applications.status_updated_at IS 'Timestamp of the last status transition. Separate from updated_at to support status age queries.';

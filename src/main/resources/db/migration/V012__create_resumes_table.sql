CREATE TABLE IF NOT EXISTS resumes (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    student_id          UUID            NOT NULL,
    label               VARCHAR(255)    NOT NULL,
    original_filename   VARCHAR(500)    NOT NULL,
    s3_key              VARCHAR(500)    NOT NULL,
    file_size_bytes     BIGINT          NOT NULL,
    is_default          BOOLEAN         NOT NULL DEFAULT FALSE,
    uploaded_at         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ     NULL,

    CONSTRAINT pk_resumes PRIMARY KEY (id),
    CONSTRAINT fk_resumes_student FOREIGN KEY (student_id) REFERENCES student_profiles (id) ON DELETE RESTRICT,
    CONSTRAINT uq_resumes_s3_key UNIQUE (s3_key),
    CONSTRAINT chk_resumes_file_size CHECK (file_size_bytes > 0 AND file_size_bytes <= 10485760)
);

-- Partial unique index: at most one active (non-deleted) resume per student may be the default.
CREATE UNIQUE INDEX IF NOT EXISTS uq_resumes_student_default
    ON resumes (student_id)
    WHERE is_default = TRUE AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_resumes_student_id ON resumes (student_id) WHERE deleted_at IS NULL;

COMMENT ON TABLE  resumes          IS 'Metadata for student resume files stored on AWS S3. Actual PDF bytes are not stored here.';
COMMENT ON COLUMN resumes.s3_key   IS 'AWS S3 object key. Format: resumes/{studentId}/{resumeId}/{filename}. Used to generate pre-signed download URLs.';
COMMENT ON COLUMN resumes.deleted_at IS 'Soft delete. Deleted resumes remain to preserve historical application references.';

CREATE TABLE IF NOT EXISTS student_experiences (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    student_id      UUID            NOT NULL,
    company_name    VARCHAR(255)    NOT NULL,
    role            VARCHAR(255)    NOT NULL,
    description     TEXT            NULL,
    start_date      DATE            NOT NULL,
    end_date        DATE            NULL,
    is_current      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_student_experiences PRIMARY KEY (id),
    CONSTRAINT fk_student_experiences_student FOREIGN KEY (student_id) REFERENCES student_profiles (id) ON DELETE CASCADE,
    CONSTRAINT chk_student_experiences_dates CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT chk_student_experiences_current_consistency CHECK (
        (is_current = TRUE  AND end_date IS NULL)
        OR (is_current = FALSE AND end_date IS NOT NULL)
        OR (is_current = FALSE AND end_date IS NULL)
    )
);

CREATE TRIGGER trg_student_experiences_updated_at
    BEFORE UPDATE ON student_experiences
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE student_experiences IS 'Work and internship experience history. is_current=TRUE implies end_date IS NULL.';

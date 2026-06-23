CREATE TABLE IF NOT EXISTS student_educations (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    student_id          UUID            NOT NULL,
    degree              VARCHAR(255)    NOT NULL,
    institution         VARCHAR(255)    NOT NULL,
    field_of_study      VARCHAR(255)    NULL,
    start_year          SMALLINT        NOT NULL,
    end_year            SMALLINT        NULL,
    percentage_or_cgpa  NUMERIC(5, 2)   NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_student_educations PRIMARY KEY (id),
    CONSTRAINT fk_student_educations_student FOREIGN KEY (student_id) REFERENCES student_profiles (id) ON DELETE CASCADE,
    CONSTRAINT chk_student_educations_years      CHECK (end_year IS NULL OR end_year >= start_year),
    CONSTRAINT chk_student_educations_start_year CHECK (start_year >= 1980 AND start_year <= 2100),
    CONSTRAINT chk_student_educations_marks      CHECK (percentage_or_cgpa IS NULL OR percentage_or_cgpa >= 0)
);

CREATE TRIGGER trg_student_educations_updated_at
    BEFORE UPDATE ON student_educations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE student_educations IS 'Academic qualifications belonging to a student profile.';

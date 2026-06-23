CREATE TABLE IF NOT EXISTS student_profiles (
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id                 UUID            NOT NULL,
    first_name              VARCHAR(100)    NOT NULL,
    last_name               VARCHAR(100)    NOT NULL,
    phone                   VARCHAR(20)     NULL,
    date_of_birth           DATE            NULL,
    gender                  gender_type     NULL,
    institution             VARCHAR(255)    NOT NULL,
    department              VARCHAR(255)    NOT NULL,
    graduation_year         SMALLINT        NOT NULL,
    cgpa                    NUMERIC(3, 2)   NULL,
    bio                     TEXT            NULL,
    profile_picture_s3_key  VARCHAR(500)    NULL,
    is_profile_public       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_student_profiles PRIMARY KEY (id),
    CONSTRAINT fk_student_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT uq_student_profiles_user_id UNIQUE (user_id),
    CONSTRAINT chk_student_profiles_cgpa CHECK (cgpa IS NULL OR (cgpa >= 0.00 AND cgpa <= 10.00)),
    CONSTRAINT chk_student_profiles_graduation_year CHECK (graduation_year >= 2000 AND graduation_year <= 2100),
    CONSTRAINT chk_student_profiles_phone CHECK (phone IS NULL OR LENGTH(TRIM(phone)) >= 7)
);

CREATE INDEX IF NOT EXISTS idx_student_profiles_department      ON student_profiles (department);
CREATE INDEX IF NOT EXISTS idx_student_profiles_cgpa            ON student_profiles (cgpa)            WHERE cgpa IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_student_profiles_institution     ON student_profiles (institution);
CREATE INDEX IF NOT EXISTS idx_student_profiles_graduation_year ON student_profiles (graduation_year);

CREATE TRIGGER trg_student_profiles_updated_at
    BEFORE UPDATE ON student_profiles
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE  student_profiles                          IS '1:1 extension of users for students. Holds placement-relevant profile fields.';
COMMENT ON COLUMN student_profiles.profile_picture_s3_key  IS 'AWS S3 object key. Access via pre-signed URL; never stored as a public URL.';
COMMENT ON COLUMN student_profiles.cgpa                    IS 'Current CGPA on a 0–10 scale. NULL if student has not filled this field.';

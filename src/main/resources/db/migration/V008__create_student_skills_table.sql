CREATE TABLE IF NOT EXISTS student_skills (
    id          UUID            NOT NULL DEFAULT gen_random_uuid(),
    student_id  UUID            NOT NULL,
    skill_name  VARCHAR(100)    NOT NULL,

    CONSTRAINT pk_student_skills PRIMARY KEY (id),
    CONSTRAINT fk_student_skills_student FOREIGN KEY (student_id) REFERENCES student_profiles (id) ON DELETE CASCADE,
    CONSTRAINT uq_student_skills_student_skill UNIQUE (student_id, skill_name)
);

CREATE INDEX IF NOT EXISTS idx_student_skills_skill_name ON student_skills (skill_name);
CREATE INDEX IF NOT EXISTS idx_student_skills_student_id ON student_skills (student_id);

COMMENT ON TABLE student_skills IS 'Skills on a student profile. One row per skill to allow indexed lookups.';

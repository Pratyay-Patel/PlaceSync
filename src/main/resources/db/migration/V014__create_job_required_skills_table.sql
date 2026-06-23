CREATE TABLE IF NOT EXISTS job_required_skills (
    id          UUID            NOT NULL DEFAULT gen_random_uuid(),
    job_id      UUID            NOT NULL,
    skill_name  VARCHAR(100)    NOT NULL,

    CONSTRAINT pk_job_required_skills PRIMARY KEY (id),
    CONSTRAINT fk_job_required_skills_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE,
    CONSTRAINT uq_job_required_skills_job_skill UNIQUE (job_id, skill_name)
);

CREATE INDEX IF NOT EXISTS idx_job_required_skills_skill_name ON job_required_skills (skill_name);
CREATE INDEX IF NOT EXISTS idx_job_required_skills_job_id     ON job_required_skills (job_id);

COMMENT ON TABLE job_required_skills IS 'Required skills for a job. Normalised to support indexed skill-based job search.';

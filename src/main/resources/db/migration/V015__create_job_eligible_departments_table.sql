CREATE TABLE IF NOT EXISTS job_eligible_departments (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    job_id          UUID            NOT NULL,
    department_name VARCHAR(255)    NOT NULL,

    CONSTRAINT pk_job_eligible_departments PRIMARY KEY (id),
    CONSTRAINT fk_job_eligible_departments_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE,
    CONSTRAINT uq_job_eligible_departments_job_dept UNIQUE (job_id, department_name)
);

CREATE INDEX IF NOT EXISTS idx_job_eligible_departments_job_id ON job_eligible_departments (job_id);

COMMENT ON TABLE job_eligible_departments IS 'Departments eligible to apply for a job. Empty = all departments eligible.';

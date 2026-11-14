--liquibase formatted sql

--changeset liabilities:add-scheduler-run-trace
CREATE TABLE scheduler_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_name VARCHAR(100) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    status VARCHAR(16) NOT NULL
);

ALTER TABLE scheduler_runs
    ADD CONSTRAINT scheduler_runs_status_check CHECK (status IN ('STARTED', 'COMPLETED', 'FAILED'));

CREATE INDEX idx_scheduler_runs_job_name_started_at ON scheduler_runs (job_name, started_at DESC);
--rollback DROP TABLE IF EXISTS scheduler_runs;

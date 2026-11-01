CREATE TABLE IF NOT EXISTS simulation_runs (
    id UUID PRIMARY KEY,
    status VARCHAR(20) NOT NULL CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED')),
    scenario_name VARCHAR(200) NOT NULL,
    seed BIGINT NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    scenario_json JSONB NOT NULL,
    result_json JSONB,
    error_message TEXT,
    virtual_time_micros BIGINT NOT NULL DEFAULT 0,
    processed_events BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_simulation_runs_submitted_at ON simulation_runs (submitted_at DESC);
CREATE INDEX IF NOT EXISTS idx_simulation_runs_status ON simulation_runs (status);

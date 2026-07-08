CREATE TABLE IF NOT EXISTS agent_tasks (
    id          TEXT PRIMARY KEY,
    context_id  TEXT,
    state       TEXT,
    payload     JSONB NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_agent_tasks_state ON agent_tasks(state);
CREATE INDEX IF NOT EXISTS idx_agent_tasks_updated_at ON agent_tasks(updated_at DESC);


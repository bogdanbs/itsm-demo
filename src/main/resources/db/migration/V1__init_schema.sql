CREATE SCHEMA IF NOT EXISTS itsm;

-- Tickets
CREATE TABLE IF NOT EXISTS itsm.tickets (
    id UUID PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL,
    priority VARCHAR(30) NOT NULL,
    assignee_id UUID,
    pending_user_since TIMESTAMP(6) WITH TIME ZONE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE,
    due_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP(6) WITH TIME ZONE,
    closed_at TIMESTAMP(6) WITH TIME ZONE,
    version INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_ticket_status_created ON itsm.tickets (status, created_at);

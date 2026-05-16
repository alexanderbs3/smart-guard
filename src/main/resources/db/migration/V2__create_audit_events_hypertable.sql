CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;

DROP TABLE IF EXISTS audit_events;

CREATE TABLE audit_events (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id),
    actor_id        VARCHAR(255) NOT NULL,
    actor_type      VARCHAR(50)  NOT NULL,
    session_id      VARCHAR(255),
    action          VARCHAR(255) NOT NULL,
    resource_type   VARCHAR(255) NOT NULL,
    resource_id     VARCHAR(255),
    ip_address      INET,
    user_agent      TEXT,
    metadata        JSONB        NOT NULL DEFAULT '{}',
    event_hash      VARCHAR(64)  NOT NULL,
    previous_hash   VARCHAR(64)  NOT NULL,
    risk_score      SMALLINT     NOT NULL DEFAULT 0 CHECK (risk_score BETWEEN 0 AND 100),
    risk_level      VARCHAR(20)  NOT NULL DEFAULT 'LOW',
    occurred_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, occurred_at)
);

SELECT create_hypertable('audit_events', 'occurred_at', chunk_time_interval => INTERVAL '7 days');


CREATE INDEX idx_audit_events_tenant_time  ON audit_events(tenant_id, occurred_at DESC);
CREATE INDEX idx_audit_events_actor        ON audit_events(tenant_id, actor_id, occurred_at DESC);
CREATE INDEX idx_audit_events_action       ON audit_events(tenant_id, action, occurred_at DESC);
CREATE INDEX idx_audit_events_risk         ON audit_events(tenant_id, risk_level, occurred_at DESC);
CREATE INDEX idx_audit_events_metadata_gin ON audit_events USING GIN(metadata);
CREATE INDEX idx_audit_events_hash         ON audit_events(event_hash);

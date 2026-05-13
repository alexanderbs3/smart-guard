-- ============================================================
-- V2 — Audit Events (Hypertable TimescaleDB)
-- A tabela central do sistema — imutável por design
-- ============================================================

CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;

CREATE TABLE audit_events (
                              id              UUID        NOT NULL DEFAULT gen_random_uuid(),
                              tenant_id       UUID        NOT NULL REFERENCES tenants(id),

    -- Quem fez a ação
                              actor_id        VARCHAR(255) NOT NULL,  -- ID do usuário no sistema externo
                              actor_type      VARCHAR(50)  NOT NULL,  -- USER, SERVICE, SYSTEM
                              session_id      VARCHAR(255),

    -- O que foi feito
                              action          VARCHAR(255) NOT NULL,  -- ex: "USER_LOGIN", "DATA_EXPORT"
                              resource_type   VARCHAR(255) NOT NULL,  -- ex: "CUSTOMER_PROFILE"
                              resource_id     VARCHAR(255),           -- ex: UUID do recurso acessado

    -- Contexto
                              ip_address      INET,
                              user_agent      TEXT,
                              metadata        JSONB        NOT NULL DEFAULT '{}',

    -- Integridade criptográfica (hash chain)
                              event_hash      VARCHAR(64)  NOT NULL UNIQUE,  -- SHA-256 deste evento
                              previous_hash   VARCHAR(64)  NOT NULL,          -- hash do evento anterior (ou "GENESIS")

    -- Análise de risco
                              risk_score      SMALLINT     NOT NULL DEFAULT 0 CHECK (risk_score BETWEEN 0 AND 100),
                              risk_level      VARCHAR(20)  NOT NULL DEFAULT 'LOW', -- LOW, MEDIUM, HIGH, CRITICAL

    -- Timestamp (particionamento do TimescaleDB)
                              occurred_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

                              PRIMARY KEY (id, occurred_at)
);

-- Converte para Hypertable particionado por tempo (chunks de 7 dias)
SELECT create_hypertable('audit_events', 'occurred_at', chunk_time_interval => INTERVAL '7 days');

-- Compressão automática de chunks com mais de 30 dias
SELECT add_compression_policy('audit_events', INTERVAL '30 days');

-- ── Índices de performance ────────────────────────────────────
CREATE INDEX idx_audit_events_tenant_time  ON audit_events(tenant_id, occurred_at DESC);
CREATE INDEX idx_audit_events_actor        ON audit_events(tenant_id, actor_id, occurred_at DESC);
CREATE INDEX idx_audit_events_action       ON audit_events(tenant_id, action, occurred_at DESC);
CREATE INDEX idx_audit_events_risk         ON audit_events(tenant_id, risk_level, occurred_at DESC);
CREATE INDEX idx_audit_events_metadata_gin ON audit_events USING GIN(metadata);
CREATE INDEX idx_audit_events_hash         ON audit_events(event_hash);
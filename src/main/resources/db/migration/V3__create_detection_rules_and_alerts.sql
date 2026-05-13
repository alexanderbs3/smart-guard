-- ============================================================
-- V3 — Detection Rules e Alerts
-- ============================================================

-- ── Detection Rules ───────────────────────────────────────────
CREATE TABLE detection_rules (
                                 id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
                                 tenant_id       UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
                                 name            VARCHAR(255) NOT NULL,
                                 description     TEXT,
                                 rule_type       VARCHAR(50)  NOT NULL, -- THRESHOLD, TIME_WINDOW, ANOMALY, COMPOSITE
                                 parameters      JSONB        NOT NULL DEFAULT '{}',
    -- Ex para THRESHOLD: {"action": "DATA_EXPORT", "count": 100, "window_minutes": 60}
                                 risk_score_delta SMALLINT    NOT NULL DEFAULT 10,
                                 enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
                                 created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                                 updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── Alerts ───────────────────────────────────────────────────
CREATE TABLE alerts (
                        id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
                        tenant_id       UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
                        rule_id         UUID        REFERENCES detection_rules(id),
                        actor_id        VARCHAR(255) NOT NULL,
                        risk_level      VARCHAR(20)  NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
                        risk_score      SMALLINT     NOT NULL,
                        title           VARCHAR(255) NOT NULL,
                        description     TEXT,
                        context         JSONB        NOT NULL DEFAULT '{}',
                        status          VARCHAR(50)  NOT NULL DEFAULT 'OPEN', -- OPEN, ACKNOWLEDGED, RESOLVED, FALSE_POSITIVE
                        resolved_by     UUID         REFERENCES users(id),
                        resolved_at     TIMESTAMPTZ,
                        created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── Notification Channels ─────────────────────────────────────
CREATE TABLE notification_channels (
                                       id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
                                       tenant_id       UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
                                       name            VARCHAR(255) NOT NULL,
                                       type            VARCHAR(50)  NOT NULL, -- EMAIL, SLACK_WEBHOOK, HTTP_WEBHOOK
                                       config          JSONB        NOT NULL, -- { "url": "...", "headers": {...} }
                                       enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
                                       alert_levels    TEXT[]       NOT NULL DEFAULT ARRAY['HIGH', 'CRITICAL'],
                                       created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── Índices ───────────────────────────────────────────────────
CREATE INDEX idx_detection_rules_tenant ON detection_rules(tenant_id);
CREATE INDEX idx_alerts_tenant_status   ON alerts(tenant_id, status, created_at DESC);
CREATE INDEX idx_alerts_actor           ON alerts(tenant_id, actor_id, created_at DESC);
CREATE INDEX idx_notif_channels_tenant  ON notification_channels(tenant_id);
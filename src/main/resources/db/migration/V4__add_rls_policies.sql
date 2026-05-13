-- ============================================================
-- V4 — Row-Level Security (Multi-Tenancy)
-- Garante isolamento total entre tenants no nível do banco
-- O tenantId é injetado via SET LOCAL app.current_tenant
-- ============================================================

ALTER TABLE audit_events        ENABLE ROW LEVEL SECURITY;
ALTER TABLE detection_rules     ENABLE ROW LEVEL SECURITY;
ALTER TABLE alerts              ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_channels ENABLE ROW LEVEL SECURITY;
ALTER TABLE users               ENABLE ROW LEVEL SECURITY;

-- Política: cada tenant vê apenas seus próprios dados
CREATE POLICY tenant_isolation ON audit_events
    USING (tenant_id = current_setting('app.current_tenant')::UUID);

CREATE POLICY tenant_isolation ON detection_rules
    USING (tenant_id = current_setting('app.current_tenant')::UUID);

CREATE POLICY tenant_isolation ON alerts
    USING (tenant_id = current_setting('app.current_tenant')::UUID);

CREATE POLICY tenant_isolation ON notification_channels
    USING (tenant_id = current_setting('app.current_tenant')::UUID);

CREATE POLICY tenant_isolation ON users
    USING (tenant_id = current_setting('app.current_tenant')::UUID);
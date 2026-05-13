-- ============================================================
-- V1 — Tenants e Usuários
-- Multi-tenancy: cada organização é um tenant isolado
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── Tenants ──────────────────────────────────────────────────
CREATE TABLE tenants (
                         id             UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
                         name           VARCHAR(255) NOT NULL,
                         slug           VARCHAR(100) NOT NULL UNIQUE,   -- identificador único ex: "acme-corp"
                         plan           VARCHAR(50)  NOT NULL DEFAULT 'STARTER', -- STARTER, GROWTH, BUSINESS, ENTERPRISE
                         api_key_hash   VARCHAR(255),                    -- hash da API Key para sistemas externos
                         max_events_month BIGINT     NOT NULL DEFAULT 10000,
                         active         BOOLEAN      NOT NULL DEFAULT TRUE,
                         created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                         updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── Usuários (internos do SmartGuard) ────────────────────────
CREATE TABLE users (
                       id             UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
                       tenant_id      UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
                       email          VARCHAR(255) NOT NULL,
                       password_hash  VARCHAR(255) NOT NULL,
                       full_name      VARCHAR(255) NOT NULL,
                       role           VARCHAR(50)  NOT NULL DEFAULT 'VIEWER', -- SUPER_ADMIN, TENANT_ADMIN, ANALYST, VIEWER
                       active         BOOLEAN      NOT NULL DEFAULT TRUE,
                       last_login_at  TIMESTAMPTZ,
                       created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                       updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

                       UNIQUE (tenant_id, email)
);

-- ── Refresh Tokens ────────────────────────────────────────────
CREATE TABLE refresh_tokens (
                                id             UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
                                user_id        UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                token_hash     VARCHAR(255) NOT NULL UNIQUE,
                                expires_at     TIMESTAMPTZ  NOT NULL,
                                revoked        BOOLEAN      NOT NULL DEFAULT FALSE,
                                created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── Índices ───────────────────────────────────────────────────
CREATE INDEX idx_users_tenant_id   ON users(tenant_id);
CREATE INDEX idx_users_email       ON users(email);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_tenants_slug      ON tenants(slug);
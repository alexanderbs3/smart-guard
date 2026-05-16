-- Converte ip_address de INET para VARCHAR(45) em audit_events
-- INET causa incompatibilidade com o mapeamento String do Hibernate
-- VARCHAR(45) suporta IPv4 (15 chars) e IPv6 (45 chars)
ALTER TABLE audit_events ALTER COLUMN ip_address TYPE VARCHAR(45) USING ip_address::text;

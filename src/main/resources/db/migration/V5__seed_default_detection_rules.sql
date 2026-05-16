
INSERT INTO tenants (id, name, slug, plan, active)
VALUES ('00000000-0000-0000-0000-000000000001', 'SmartGuard System', 'smartguard-system', 'ENTERPRISE', true)
    ON CONFLICT (slug) DO NOTHING;

-- Regra 1: Exfiltração de dados — exportações em massa
INSERT INTO detection_rules (tenant_id, name, description, rule_type, parameters, risk_score_delta, enabled)
VALUES (
           '00000000-0000-0000-0000-000000000001',
           'Data Exfiltration - Export Volume',
           'Detecta mais de 50 exportações em 1 hora. Caso de uso: funcionário copiando dados antes de pedir demissão.',
           'THRESHOLD',
           '{"action": "DATA_EXPORT", "count": 50, "windowMinutes": 60}',
           35,
           true
       );

-- Regra 2: Acesso em horário atípico
INSERT INTO detection_rules (tenant_id, name, description, rule_type, parameters, risk_score_delta, enabled)
VALUES (
           '00000000-0000-0000-0000-000000000001',
           'Off-Hours Access',
           'Detecta acessos entre 22h e 6h. Acesso noturno incomum é indicativo de comprometimento de conta.',
           'TIME_WINDOW',
           '{"startHour": 22, "endHour": 6}',
           20,
           true
       );

-- Regra 3: Volume anômalo por minuto
INSERT INTO detection_rules (tenant_id, name, description, rule_type, parameters, risk_score_delta, enabled)
VALUES (
           '00000000-0000-0000-0000-000000000001',
           'High Volume Anomaly',
           'Detecta mais de 200 eventos/minuto de um único ator. Indica automação maliciosa ou scraping.',
           'HIGH_VOLUME',
           '{"maxEventsPerMinute": 200}',
           40,
           true
       );

-- Regra 4: Logins em massa (brute force / credential stuffing)
INSERT INTO detection_rules (tenant_id, name, description, rule_type, parameters, risk_score_delta, enabled)
VALUES (
           '00000000-0000-0000-0000-000000000001',
           'Login Brute Force',
           'Detecta mais de 10 tentativas de login em 5 minutos.',
           'THRESHOLD',
           '{"action": "USER_LOGIN", "count": 10, "windowMinutes": 5}',
           50,
           true
       );
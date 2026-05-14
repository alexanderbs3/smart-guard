# SmartGuard 🛡️

**Behavioral Audit & Anomaly Detection Platform**

> *"Every action leaves a trace. SmartGuard makes sure nothing is forgotten — and nothing can be falsified."*

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red?logo=redis)](https://redis.io/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-7.5-black?logo=apachekafka)](https://kafka.apache.org/)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

---

## O que é

SmartGuard é uma plataforma SaaS B2B de **auditoria comportamental e detecção de anomalias em tempo real**.

Sistemas externos enviam eventos de atividade de usuários — logins, acessos a dados, exportações — e o SmartGuard:

- Armazena em **audit trail imutável** com hash SHA-256 encadeado
- Calcula **risk score em tempo real** por ator via Redis sliding window
- Avalia **regras de detecção** configuráveis por tenant sem redeploy
- Dispara **alertas automáticos** via REST + **WebSocket sub-segundo**

**Problema que resolve:** organizações que precisam de compliance (LGPD, SOC 2, ISO 27001, PCI-DSS) sem o custo proibitivo de soluções como Splunk ou Datadog.

---

## Arquitetura

```
Sistemas Externos
      │
      ├─── REST  POST /api/v1/events
      └─── Kafka smartguard.audit-events
                    │
                    ▼
         ┌─────────────────────┐
         │   IngestEventService │
         │                     │
         │  1. HashChain        │  ← SHA-256 encadeado (imutável)
         │  2. RiskScore        │  ← Redis ZSET sliding window
         │  3. Persist          │  ← TimescaleDB hypertable
         │  4. RuleEngine       │  ← Strategy pattern (plugável)
         │  5. AlertService     │  ← DB + WebSocket push
         └─────────────────────┘
                    │
         ┌──────────┴──────────┐
         │                     │
    PostgreSQL             Redis
    + TimescaleDB          Sorted Sets
    + RLS multi-tenant     Sliding Windows
```

---

## Stack Tecnológica

| Camada | Tecnologia | Motivo |
|---|---|---|
| Runtime | Java 21 LTS | Virtual Threads, Records, Pattern Matching |
| Framework | Spring Boot 3.3 | Ecossistema maduro, production-ready |
| Segurança | Spring Security 6 + JWT | RBAC com `@PreAuthorize`, stateless |
| Banco | PostgreSQL 16 + TimescaleDB | ACID + hypertable temporal + RLS multi-tenant |
| Cache/Score | Redis 7 (Sorted Sets) | Sliding window O(log N) |
| Mensageria | Apache Kafka 7.5 | Ingestão massiva, reprocessamento, at-least-once |
| Migrations | Flyway 9 | Schema versionado e auditável |
| Realtime | Spring WebSocket (STOMP) | Alertas sub-segundo |
| Resiliência | Resilience4j 2 | Circuit Breaker, Retry, Rate Limiter |
| Mapeamento | MapStruct 1.5 | Zero reflection em compile time |
| Métricas | Micrometer + Prometheus | p50/p95/p99 por tenant |
| Docs | SpringDoc OpenAPI 2 | Swagger UI automático |

---

## Funcionalidades

### Audit Trail Imutável
Cada evento recebe um `event_hash = SHA256(previousHash + campos)`. Qualquer adulteração em qualquer campo invalida toda a cadeia subsequente. Verificável externamente sem acesso ao sistema.

### Risk Score em Tempo Real
Redis Sorted Sets com timestamps como score. Janela deslizante de 60 min. Score 0–100 normalizado. Atualizado em cada evento, disponível em < 5ms.

### Rule Engine Plugável (Strategy Pattern)
Regras configuradas por tenant via API — sem redeploy. Tipos disponíveis:

| Tipo | Parâmetros | Exemplo |
|---|---|---|
| `THRESHOLD` | action, count, windowMinutes | 50 exports/hora → alerta |
| `TIME_WINDOW` | startHour, endHour | Login às 3h → suspeito |
| `HIGH_VOLUME` | maxEventsPerMinute | 200 eventos/min → bot |

Para adicionar novo tipo: implementar `RuleEvaluator` + `@Component`. Zero modificação no engine.

### Multi-Tenancy com Row-Level Security
Isolamento garantido no nível do banco via PostgreSQL RLS. Nenhuma query pode vazar dados de outro tenant — mesmo com bug na aplicação.

### Alertas em Tempo Real
WebSocket STOMP. Clientes subscrevem `/topic/alerts/{tenantId}`. Latência evento → alerta → cliente: < 100ms.

---

## Quick Start

### Pré-requisitos
- Docker e Docker Compose
- Java 21+ (para desenvolvimento local)

### Subir infraestrutura

```bash
git clone https://github.com/seu-usuario/smartguard
cd smartguard
cp .env.example .env    # edite os valores
docker compose up -d postgres redis kafka zookeeper kafka-ui
```

### Rodar a aplicação

```bash
./mvnw spring-boot:run
```

Acesse:
- **API:** `http://localhost:8080`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **Kafka UI:** `http://localhost:8090`
- **Health:** `http://localhost:8080/api/v1/health`

---

## Exemplos de Uso

### 1. Registrar tenant e obter token

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "tenantName": "Acme Corp",
    "tenantSlug": "acme-corp",
    "fullName": "Admin User",
    "email": "admin@acme.com",
    "password": "senha123456"
  }' | jq .accessToken
```

### 2. Ingerir evento de auditoria

```bash
TOKEN="<token-do-passo-anterior>"

curl -X POST http://localhost:8080/api/v1/events \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "actorId": "user-42",
    "actorType": "USER",
    "action": "DATA_EXPORT",
    "resourceType": "CUSTOMER_PROFILE",
    "resourceId": "cust-999",
    "ipAddress": "192.168.1.100",
    "metadata": { "format": "CSV", "rows": 15000 }
  }'
```

**Resposta:**
```json
{
  "id": "...",
  "actorId": "user-42",
  "action": "DATA_EXPORT",
  "eventHash": "a3f8c2...",
  "previousHash": "GENESIS",
  "riskScore": 12,
  "riskLevel": "LOW",
  "occurredAt": "2024-11-15T14:32:01Z"
}
```

### 3. Criar regra de detecção

```bash
curl -X POST http://localhost:8080/api/v1/rules \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Exportação em massa",
    "ruleType": "THRESHOLD",
    "parameters": { "action": "DATA_EXPORT", "count": 10, "windowMinutes": 60 },
    "riskScoreDelta": 35
  }'
```

### 4. Conectar via WebSocket (JavaScript)

```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stomp  = Stomp.over(socket);

stomp.connect({ Authorization: `Bearer ${token}` }, () => {
  stomp.subscribe(`/topic/alerts/${tenantId}`, (msg) => {
    const alert = JSON.parse(msg.body);
    console.log(`🚨 ${alert.riskLevel}: ${alert.title}`);
  });
});
```

---

## Estrutura do Projeto

```
src/main/java/br/leetjourney/smartguard/
├── domain/
│   ├── model/              → AuditEvent, Alert, DetectionRule, Tenant, User
│   ├── repository/         → Interfaces JPA
│   └── service/
│       ├── HashChainService.java       ← SHA-256 encadeado
│       ├── RiskScoreService.java       ← Redis sliding window
│       └── ruleengine/
│           ├── RuleEvaluator.java      ← Interface Strategy
│           ├── RuleEngine.java         ← Orquestrador
│           ├── ThresholdRuleEvaluator.java
│           ├── TimeWindowRuleEvaluator.java
│           └── HighVolumeRuleEvaluator.java
├── application/
│   ├── service/            → IngestEventService, AlertService, AuthService...
│   └── dto/                → Records Java 21
├── infrastructure/
│   ├── security/           → JwtService, JwtAuthenticationFilter
│   ├── multitenancy/       → TenantContext, TenantAwareInterceptor
│   ├── messaging/kafka/    → AuditEventKafkaConsumer, KafkaEventMessage
│   ├── observability/      → SmartGuardMetrics
│   └── config/             → SecurityConfig, KafkaConfig, WebSocketConfig...
└── interfaces/rest/        → Controllers + GlobalExceptionHandler
```

---

## Segurança

### RBAC (Role-Based Access Control)

| Role | Permissões |
|---|---|
| `SUPER_ADMIN` | Acesso a todos os tenants |
| `TENANT_ADMIN` | CRUD de usuários, regras e canais do próprio tenant |
| `ANALYST` | Ingestão, leitura de eventos e alertas, verificação de cadeia |
| `VIEWER` | Leitura de dashboard e alertas |
| `API_KEY` | Apenas ingestão de eventos (sistemas externos) |

### Hash Chain

Cada evento carrega `event_hash = SHA256(previousHash | tenantId | actorId | actorType | action | resourceType | resourceId | occurredAt)`.

Para verificar a integridade:
```
GET /api/v1/events?verify=true
```
Retorna `chainValid: true/false` e o índice do primeiro evento adulterado, se houver.

---

## Testes

```bash
# Unitários
./mvnw test -Dtest="!*IntegrationTest"

# Integração (precisa de Docker)
./mvnw test -Dtest="*IntegrationTest"

# Todos
./mvnw verify

# Load test (precisa de k6 instalado)
k6 run --env TEST_JWT=<token> k6/ingest-load-test.js
```

**Cobertura de testes:**
- `HashChainServiceTest` — determinismo, adulteração, cadeia GENESIS
- `RiskScoreServiceTest` — isolamento por tenant, normalização 0–100
- `RuleEngineTest` — todos os tipos, múltiplas regras, zero regras
- `JwtServiceTest` — geração, claims, expiração, adulteração
- `AuthControllerIntegrationTest` — fluxo completo register/login/errors

---

## Observabilidade

### Métricas (Prometheus)
```
smartguard_events_ingested_total{tenant, risk_level}
smartguard_alerts_triggered_total{tenant}
smartguard_risk_score{tenant, quantile="0.99"}
smartguard_ingest_duration_seconds{tenant, quantile="0.99"}
```

### Subir Prometheus + Grafana
```bash
docker compose -f docker-compose.observability.yml up -d
```

- **Prometheus:** `http://localhost:9090`
- **Grafana:** `http://localhost:3000` (admin / admin)

---

## Roadmap

- [x] Fase 1 — Fundação (Docker, Flyway, Multi-tenancy, JWT)
- [x] Fase 2 — Core (Hash Chain, Risk Score, Ingestão REST + Kafka)
- [x] Fase 3 — Inteligência (Rule Engine, Alertas, WebSocket)
- [x] Fase 4 — Observabilidade (Micrometer, Prometheus, GitHub Actions)
- [ ] Fase 5 — Chain Verification endpoint (exportação de evidências)
- [ ] Fase 6 — Notification Channels (Email, Slack, Webhook)
- [ ] Fase 7 — Dashboard UI (React + WebSocket)

---

## Licença

MIT © SmartGuard Engineering
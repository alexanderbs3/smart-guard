// k6/ingest-load-test.js
// Execução: k6 run --env TEST_JWT=<token> k6/ingest-load-test.js
//
// Stages: ramp up → sustain → pico → ramp down
// SLO target: p99 < 100ms, error rate < 1%

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// ── Métricas customizadas ─────────────────────────────────────
const ingestErrors  = new Counter('smartguard_ingest_errors');
const ingestSuccess = new Counter('smartguard_ingest_success');
const errorRate     = new Rate('smartguard_error_rate');
const ingestLatency = new Trend('smartguard_ingest_latency', true);

// ── Configuração de carga ──────────────────────────────────────
export const options = {
    stages: [
        { duration: '2m',  target: 100  }, // Ramp up
        { duration: '5m',  target: 500  }, // Sustain
        { duration: '2m',  target: 1000 }, // Pico
        { duration: '1m',  target: 0    }, // Ramp down
    ],
    thresholds: {
        'http_req_duration':             ['p(99)<100'],  // p99 < 100ms
        'http_req_failed':               ['rate<0.01'],  // < 1% de erros
        'smartguard_ingest_latency':     ['p(99)<100'],
        'smartguard_error_rate':         ['rate<0.01'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const JWT      = __ENV.TEST_JWT;

const ACTIONS = ['DATA_EXPORT', 'USER_LOGIN', 'RECORD_VIEW', 'REPORT_DOWNLOAD', 'SETTINGS_CHANGE'];
const RESOURCES = ['CUSTOMER_PROFILE', 'FINANCIAL_RECORD', 'AUDIT_LOG', 'USER_ACCOUNT'];

function randomItem(arr) {
    return arr[Math.floor(Math.random() * arr.length)];
}

// ── Teste principal ────────────────────────────────────────────
export default function () {
    const actorId    = `user-${Math.floor(Math.random() * 1000)}`;
    const resourceId = `rec-${Math.floor(Math.random() * 10000)}`;

    const payload = JSON.stringify({
        actorId:      actorId,
        actorType:    'USER',
        sessionId:    `sess-${Date.now()}`,
        action:       randomItem(ACTIONS),
        resourceType: randomItem(RESOURCES),
        resourceId:   resourceId,
        ipAddress:    `192.168.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`,
        metadata: {
            browser:  'Chrome/120',
            os:       'Linux',
            country:  'BR',
        },
    });

    const res = http.post(`${BASE_URL}/api/v1/events`, payload, {
        headers: {
            'Content-Type':  'application/json',
            'Authorization': `Bearer ${JWT}`,
        },
        timeout: '5s',
    });

    const ok = check(res, {
        'status 201':          (r) => r.status === 201,
        'latência < 100ms':    (r) => r.timings.duration < 100,
        'tem eventHash':       (r) => JSON.parse(r.body).eventHash !== undefined,
    });

    ingestLatency.add(res.timings.duration);
    errorRate.add(!ok);

    if (ok) {
        ingestSuccess.add(1);
    } else {
        ingestErrors.add(1);
        console.error(`Falha: status=${res.status} body=${res.body.substring(0, 200)}`);
    }

    sleep(0.1);
}
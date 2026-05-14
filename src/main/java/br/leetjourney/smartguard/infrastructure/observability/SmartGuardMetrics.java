package br.leetjourney.smartguard.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class SmartGuardMetrics {

    private final MeterRegistry registry;

    // Cache de meters para evitar lookup a cada evento (hot path)
    private final ConcurrentMap<String, Counter> eventCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, DistributionSummary> riskSummaries = new ConcurrentHashMap<>();

    public SmartGuardMetrics(MeterRegistry registry) {
        this.registry = registry;

        // Registra o counter de duplicatas uma única vez
        Counter.builder("smartguard.events.duplicate")
                .description("Eventos duplicados ignorados por idempotência")
                .register(registry);
    }

    // ── Event Ingestion ───────────────────────────────────────

    public void recordEventIngested(String tenantId, String riskLevel) {
        String key = tenantId + ":" + riskLevel;
        eventCounters.computeIfAbsent(key, k ->
                Counter.builder("smartguard.events.ingested")
                        .tag("tenant", tenantId)
                        .tag("risk_level", riskLevel)
                        .description("Total de eventos ingeridos por tenant e nível de risco")
                        .register(registry)
        ).increment();
    }

    public void recordAlertTriggered(String tenantId) {
        Counter.builder("smartguard.alerts.triggered")
                .tag("tenant", tenantId)
                .description("Total de alertas disparados por tenant")
                .register(registry)
                .increment();
    }

    // ── Risk Score Distribution ───────────────────────────────

    public void recordRiskScore(String tenantId, int score) {
        riskSummaries.computeIfAbsent(tenantId, k ->
                DistributionSummary.builder("smartguard.risk.score")
                        .tag("tenant", tenantId)
                        .description("Distribuição de risk scores — p50/p90/p99")
                        .publishPercentiles(0.5, 0.9, 0.99)
                        .scale(1)
                        .register(registry)
        ).record(score);
    }

    // ── Pipeline Timing ───────────────────────────────────────

    /**
     * Mede o tempo completo do pipeline de ingestão (hash + score + save + rules).
     * Uso:
     *   var sample = metrics.startIngestTimer();
     *   // ... pipeline ...
     *   metrics.stopIngestTimer(sample, tenantId, alertTriggered);
     */
    public Timer.Sample startIngestTimer() {
        return Timer.start(registry);
    }

    public void stopIngestTimer(Timer.Sample sample, String tenantId, boolean alertTriggered) {
        sample.stop(Timer.builder("smartguard.ingest.duration")
                .tag("tenant", tenantId)
                .tag("alert_triggered", String.valueOf(alertTriggered))
                .description("Latência end-to-end do pipeline de ingestão")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry));
    }

    public Timer.Sample startChainVerificationTimer() {
        return Timer.start(registry);
    }

    public void stopChainVerificationTimer(Timer.Sample sample) {
        sample.stop(Timer.builder("smartguard.chain.verification.duration")
                .description("Latência de verificação de integridade da hash chain")
                .register(registry));
    }

}

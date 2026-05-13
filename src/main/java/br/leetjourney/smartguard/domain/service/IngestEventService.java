package br.leetjourney.smartguard.domain.service;


import br.leetjourney.smartguard.application.dto.AuditEventResponse;
import br.leetjourney.smartguard.application.dto.IngestEventRequest;
import br.leetjourney.smartguard.domain.model.AuditEvent;
import br.leetjourney.smartguard.domain.repository.AuditEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestEventService {

    private final AuditEventRepository auditEventRepository;
    private final HashChainService hashChainService;
    private final RiskScoreService riskScoreService;
    private final MeterRegistry meterRegistry;

    @Transactional
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'ANALYST', 'API_KEY')")
    public AuditEventResponse ingest(UUID tenantId, IngestEventRequest request) {
        OffsetDateTime occurredAt = request.occurredAt() != null
                ? request.occurredAt()
                : OffsetDateTime.now();

        // ── 1. Obtém o hash anterior da cadeia ────────────────
        String previousHash = hashChainService.getLatestHash(tenantId);

        // ── 2. Monta o evento (sem hash ainda — precisamos do objeto para computar) ──
        AuditEvent proto = AuditEvent.builder()
                .tenantId(tenantId)
                .actorId(request.actorId())
                .actorType(request.actorType())
                .sessionId(request.sessionId())
                .action(request.action())
                .resourceType(request.resourceType())
                .resourceId(request.resourceId())
                .ipAddress(request.ipAddress())
                .userAgent(request.userAgent())
                .metadata(request.metadata() != null ? request.metadata() : Map.of())
                .previousHash(previousHash)
                .occurredAt(occurredAt)
                .build();

        // ── 3. Computa o hash deste evento ────────────────────
        String eventHash = hashChainService.computeHash(previousHash, proto);

        // ── 4. Idempotência: ignora duplicata silenciosamente ─
        if (auditEventRepository.existsByEventHash(eventHash)) {
            log.warn("Evento duplicado ignorado: hash={}", eventHash);
            meterRegistry.counter("smartguard.events.duplicate").increment();
            return auditEventRepository.findAll()   // workaround: busca por hash
                    .stream()
                    .filter(e -> e.getEventHash().equals(eventHash))
                    .findFirst()
                    .map(AuditEventResponse::from)
                    .orElseThrow();
        }

        // ── 5. Calcula risk score via Redis sliding window ────
        int riskScore = riskScoreService.recordAndScore(tenantId, request.actorId(), UUID.randomUUID());
        AuditEvent.RiskLevel riskLevel = AuditEvent.RiskLevel.fromScore(riskScore);

        // ── 6. Persiste o evento completo ─────────────────────
        AuditEvent event = AuditEvent.builder()
                .tenantId(tenantId)
                .actorId(request.actorId())
                .actorType(request.actorType())
                .sessionId(request.sessionId())
                .action(request.action())
                .resourceType(request.resourceType())
                .resourceId(request.resourceId())
                .ipAddress(request.ipAddress())
                .userAgent(request.userAgent())
                .metadata(request.metadata() != null ? request.metadata() : Map.of())
                .previousHash(previousHash)
                .eventHash(eventHash)
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .occurredAt(occurredAt)
                .build();

        AuditEvent saved = auditEventRepository.save(event);

        // ── 7. Métricas ───────────────────────────────────────
        meterRegistry.counter("smartguard.events.ingested",
                "tenant", tenantId.toString(),
                "risk_level", riskLevel.name()
        ).increment();

        log.info("Evento ingerido: tenant={} actor={} action={} risk={}({}) hash={}",
                tenantId, request.actorId(), request.action(),
                riskLevel, riskScore, eventHash.substring(0, 8));

        return AuditEventResponse.from(saved);
    }
}

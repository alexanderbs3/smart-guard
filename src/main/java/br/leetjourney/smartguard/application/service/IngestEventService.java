package br.leetjourney.smartguard.application.service;

import br.leetjourney.smartguard.application.dto.event.AuditEventResponse;
import br.leetjourney.smartguard.application.dto.event.IngestEventRequest;
import br.leetjourney.smartguard.domain.model.AuditEvent;
import br.leetjourney.smartguard.domain.repository.AuditEventRepository;
import br.leetjourney.smartguard.domain.service.HashChainService;
import br.leetjourney.smartguard.domain.service.RiskScoreService;
import br.leetjourney.smartguard.domain.service.ruleengine.RuleEngine;
import br.leetjourney.smartguard.domain.service.ruleengine.RuleViolation;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestEventService {

    private final AuditEventRepository auditEventRepository;
    private final HashChainService hashChainService;
    private final RiskScoreService riskScoreService;
    private final RuleEngine ruleEngine;
    private final AlertService alertService;
    private final MeterRegistry meterRegistry;

    @Transactional
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'ANALYST', 'API_KEY')")
    public AuditEventResponse ingest(UUID tenantId, IngestEventRequest request) {
        OffsetDateTime occurredAt = request.occurredAt() != null
                ? request.occurredAt()
                : OffsetDateTime.now();

        String previousHash = hashChainService.getLatestHash(tenantId);
        AuditEvent proto = buildProto(tenantId, request, previousHash, occurredAt);
        String eventHash = hashChainService.computeHash(previousHash, proto);

        if (auditEventRepository.existsByEventHash(eventHash)) {
            log.warn("Evento duplicado ignorado: hash={}", eventHash);
            meterRegistry.counter("smartguard.events.duplicate").increment();
            return auditEventRepository.findAll().stream()
                    .filter(e -> e.getEventHash().equals(eventHash))
                    .findFirst()
                    .map(AuditEventResponse::from)
                    .orElseThrow();
        }

        int riskScore = riskScoreService.recordAndScore(tenantId, request.actorId(), UUID.randomUUID());
        AuditEvent.RiskLevel riskLevel = AuditEvent.RiskLevel.fromScore(riskScore);
        AuditEvent saved = auditEventRepository.save(buildFinal(proto, eventHash, riskScore, riskLevel));

        List<RuleViolation> violations = ruleEngine.evaluate(saved);
        if (!violations.isEmpty()) {
            alertService.createFromViolations(saved, violations, riskScore);
            meterRegistry.counter("smartguard.alerts.triggered",
                    "tenant", tenantId.toString()).increment();
        }

        meterRegistry.counter("smartguard.events.ingested",
                "tenant", tenantId.toString(),
                "risk_level", riskLevel.name()
        ).increment();

        log.info("Evento ingerido: tenant={} actor={} action={} risk={}({}) violations={} hash={}",
                tenantId, request.actorId(), request.action(),
                riskLevel, riskScore, violations.size(), eventHash.substring(0, 8));

        return AuditEventResponse.from(saved);
    }

    private AuditEvent buildProto(UUID tenantId, IngestEventRequest req,
                                   String previousHash, OffsetDateTime occurredAt) {
        return AuditEvent.builder()
                .tenantId(tenantId)
                .actorId(req.actorId())
                .actorType(req.actorType())
                .sessionId(req.sessionId())
                .action(req.action())
                .resourceType(req.resourceType())
                .resourceId(req.resourceId())
                .ipAddress(req.ipAddress())
                .userAgent(req.userAgent())
                .metadata(req.metadata() != null ? req.metadata() : Map.of())
                .previousHash(previousHash)
                .occurredAt(occurredAt)
                .build();
    }

    private AuditEvent buildFinal(AuditEvent proto, String eventHash,
                                   int riskScore, AuditEvent.RiskLevel riskLevel) {
        return AuditEvent.builder()
                .tenantId(proto.getTenantId())
                .actorId(proto.getActorId())
                .actorType(proto.getActorType())
                .sessionId(proto.getSessionId())
                .action(proto.getAction())
                .resourceType(proto.getResourceType())
                .resourceId(proto.getResourceId())
                .ipAddress(proto.getIpAddress())
                .userAgent(proto.getUserAgent())
                .metadata(proto.getMetadata())
                .previousHash(proto.getPreviousHash())
                .eventHash(eventHash)
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .occurredAt(proto.getOccurredAt())
                .build();
    }
}

package br.leetjourney.smartguard.application.service;

import br.leetjourney.smartguard.application.dto.AlertNotification;
import br.leetjourney.smartguard.domain.model.Alert;
import br.leetjourney.smartguard.domain.model.AuditEvent;
import br.leetjourney.smartguard.domain.repository.AlertRepository;
import br.leetjourney.smartguard.domain.service.ruleengine.RuleViolation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {


    private static final String ALERT_TOPIC = "/topic/alerts/";

    private final AlertRepository alertRepository;
    private final SimpMessagingTemplate websocket;

    /**
     * Chamado pelo IngestEventService após avaliação do RuleEngine.
     * Persiste um Alert para cada violação e faz push via WebSocket.
     */
    @Transactional
    public List<AlertNotification> createFromViolations(AuditEvent event,
                                                        List<RuleViolation> violations,
                                                        int riskScore) {
        return violations.stream()
                .map(v -> createAndNotify(event, v, riskScore))
                .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'ANALYST', 'VIEWER')")
    public Page<AlertNotification> listByTenant(UUID tenantId, Alert.AlertStatus status,
                                                Pageable pageable) {
        Page<Alert> alerts = (status != null)
                ? alertRepository.findAllByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status, pageable)
                : alertRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId, pageable);

        return alerts.map(AlertNotification::from);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'ANALYST')")
    public AlertNotification acknowledge(UUID alertId, UUID resolvedBy) {
        Alert alert = findOrThrow(alertId);
        alert.acknowledge(resolvedBy);
        return AlertNotification.from(alertRepository.save(alert));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'ANALYST')")
    public AlertNotification resolve(UUID alertId, UUID resolvedBy) {
        Alert alert = findOrThrow(alertId);
        alert.resolve(resolvedBy);
        return AlertNotification.from(alertRepository.save(alert));
    }

    // ── Privado ───────────────────────────────────────────────

    private AlertNotification createAndNotify(AuditEvent event,
                                              RuleViolation violation,
                                              int riskScore) {
        AuditEvent.RiskLevel riskLevel = AuditEvent.RiskLevel.fromScore(riskScore);

        Alert alert = alertRepository.save(Alert.builder()
                .tenantId(event.getTenantId())
                .ruleId(violation.rule().getId())
                .actorId(event.getActorId())
                .riskLevel(riskLevel)
                .riskScore(riskScore)
                .title(violation.title())
                .description(violation.description())
                .context(violation.context())
                .status(Alert.AlertStatus.OPEN)
                .build());

        AlertNotification notification = AlertNotification.from(alert);

        // Push WebSocket: /topic/alerts/{tenantId}
        String destination = ALERT_TOPIC + event.getTenantId();
        websocket.convertAndSend(destination, notification);

        log.info("Alert criado e enviado via WS: tenant={} actor={} rule='{}' risk={}({})",
                event.getTenantId(), event.getActorId(),
                violation.rule().getName(), riskLevel, riskScore);

        return notification;
    }

    private Alert findOrThrow(UUID alertId) {
        return alertRepository.findById(alertId)
                .orElseThrow(() -> ResourceNotFoundException.of("Alert", alertId));
    }

}

package br.leetjourney.smartguard.application.dto.alert;

import br.leetjourney.smartguard.domain.model.Alert;
import br.leetjourney.smartguard.domain.model.AuditEvent;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AlertNotification(
        UUID id,
        UUID tenantId,
        UUID ruleId,
        String actorId,
        AuditEvent.RiskLevel riskLevel,
        int riskScore,
        String title,
        String description,
        Map<String, Object> context,
        Alert.AlertStatus status,
        OffsetDateTime createdAt
) {
    public static AlertNotification from(Alert alert) {
        return new AlertNotification(
                alert.getId(),
                alert.getTenantId(),
                alert.getRuleId(),
                alert.getActorId(),
                alert.getRiskLevel(),
                alert.getRiskScore(),
                alert.getTitle(),
                alert.getDescription(),
                alert.getContext(),
                alert.getStatus(),
                alert.getCreatedAt()
        );
    }
}


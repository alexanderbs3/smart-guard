package br.leetjourney.smartguard.application.dto;

import br.leetjourney.smartguard.domain.model.AuditEvent;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        String actorId,
        AuditEvent.ActorType actorType,
        String sessionId,
        String action,
        String resourceType,
        String resourceId,
        String ipAddress,
        Map<String, Object> metadata,
        String eventHash,
        String previousHash,
        int riskScore,
        AuditEvent.RiskLevel riskLevel,
        OffsetDateTime occurredAt
) {
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getActorId(),
                event.getActorType(),
                event.getSessionId(),
                event.getAction(),
                event.getResourceType(),
                event.getResourceId(),
                event.getIpAddress(),
                event.getMetadata(),
                event.getEventHash(),
                event.getPreviousHash(),
                event.getRiskScore(),
                event.getRiskLevel(),
                event.getOccurredAt()
        );
    }
}

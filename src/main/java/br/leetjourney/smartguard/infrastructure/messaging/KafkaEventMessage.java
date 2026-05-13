package br.leetjourney.smartguard.infrastructure.messaging;

import br.leetjourney.smartguard.domain.model.AuditEvent;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record KafkaEventMessage(

        UUID tenantId,
        String actorId,
        AuditEvent.ActorType actorType,
        String sessionId,
        String action,
        String resourceType,
        String resourceId,
        String ipAddress,
        String userAgent,
        Map<String, Object> metadata,
        OffsetDateTime occurredAt
) {
}

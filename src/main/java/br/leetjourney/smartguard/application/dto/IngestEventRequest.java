package br.leetjourney.smartguard.application.dto;

import br.leetjourney.smartguard.domain.model.AuditEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.Map;

public record IngestEventRequest(
        @NotBlank @Size(max = 255)
        String actorId,

        @NotNull
        AuditEvent.ActorType actorType,

        @Size(max = 255)
        String sessionId,

        @NotBlank @Size(max = 255)
        String action,

        @NotBlank @Size(max = 255)
        String resourceType,

        @Size(max = 255)
        String resourceId,

        @Size(max = 45)
        String ipAddress,

        String userAgent,

        Map<String, Object> metadata,

        @PastOrPresent
        OffsetDateTime occurredAt   // null → usa NOW()
) {}

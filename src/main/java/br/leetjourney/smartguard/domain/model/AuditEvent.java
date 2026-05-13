package br.leetjourney.smartguard.domain.model;


import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entidade central do SmartGuard — imutável por design.
 * Usa chave composta (id + occurred_at) exigida pelo TimescaleDB hypertable.
 * Nunca possui métodos de escrita fora do Builder — integridade garantida via HashChainService.
 */
@Entity
@Table(name = "audit_events")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(AuditEvent.AuditEventId.class)

public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Id
    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    // ── Ator ─────────────────────────────────────────────────
    @Column(name = "actor_id", nullable = false, length = 255)
    private String actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 50)
    private ActorType actorType;

    @Column(name = "session_id", length = 255)
    private String sessionId;

    // ── Ação ─────────────────────────────────────────────────
    @Column(nullable = false, length = 255)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 255)
    private String resourceType;

    @Column(name = "resource_id", length = 255)
    private String resourceId;

    // ── Contexto ─────────────────────────────────────────────
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, Object> metadata = Map.of();

    // ── Hash Chain (imutabilidade criptográfica) ──────────────
    @Column(name = "event_hash", nullable = false, length = 64, unique = true)
    private String eventHash;

    @Column(name = "previous_hash", nullable = false, length = 64)
    private String previousHash;

    // ── Risk ─────────────────────────────────────────────────
    @Column(name = "risk_score", nullable = false)
    @Builder.Default
    private int riskScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    @Builder.Default
    private RiskLevel riskLevel = RiskLevel.LOW;

    // ── Enums ─────────────────────────────────────────────────

    public enum ActorType { USER, SERVICE, SYSTEM }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL;

        public static RiskLevel fromScore(int score) {
            if (score >= 80) return CRITICAL;
            if (score >= 60) return HIGH;
            if (score >= 30) return MEDIUM;
            return LOW;
        }
    }

    // ── Chave composta ────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditEventId implements Serializable {
        private UUID id;
        private OffsetDateTime occurredAt;
    }
}

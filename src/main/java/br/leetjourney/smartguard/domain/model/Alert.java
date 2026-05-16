package br.leetjourney.smartguard.domain.model;


import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "alerts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "rule_id")
    private UUID ruleId;

    @Column(name = "actor_id", nullable = false, length = 255)
    private String actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private AuditEvent.RiskLevel riskLevel;

    @Column(name = "risk_score", nullable = false, columnDefinition = "smallint")
    private int riskScore;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, Object> context = Map.of();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private AlertStatus status = AlertStatus.OPEN;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    public enum AlertStatus { OPEN, ACKNOWLEDGED, RESOLVED, FALSE_POSITIVE }

    public void acknowledge(UUID userId) {
        this.status = AlertStatus.ACKNOWLEDGED;
        this.resolvedBy = userId;
    }

    public void resolve(UUID userId) {
        this.status = AlertStatus.RESOLVED;
        this.resolvedBy = userId;
        this.resolvedAt = OffsetDateTime.now();
    }
}

package br.leetjourney.smartguard.domain.service.ruleengine;

import br.leetjourney.smartguard.domain.model.AuditEvent;
import br.leetjourney.smartguard.domain.model.DetectionRule;
import br.leetjourney.smartguard.domain.repository.DetectionRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleEngineTest {

    @Mock
    DetectionRuleRepository ruleRepository;
    @Mock
    StringRedisTemplate redis;

    private RuleEngine ruleEngine;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();

        // Monta ZSetOperations mock para ThresholdRuleEvaluator e HighVolumeRuleEvaluator
        ZSetOperations<String, String> zSet = mock(ZSetOperations.class);
        when(redis.opsForZSet()).thenReturn(zSet);
        when(zSet.add(anyString(), anyString(), anyDouble())).thenReturn(true);
        when(zSet.removeRangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(0L);
        when(zSet.zCard(anyString())).thenReturn(5L); // abaixo do threshold por padrão

        List<RuleEvaluator> evaluators = List.of(
                new ThresholdRuleEvaluator(redis),
                new TimeWindowRuleEvaluator(),
                new HighVolumeRuleEvaluator(redis)
        );

        ruleEngine = new RuleEngine(ruleRepository, evaluators);
    }

    private AuditEvent buildEvent(String action, int hour) {
        return AuditEvent.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .actorId("actor-test")
                .actorType(AuditEvent.ActorType.USER)
                .action(action)
                .resourceType("CUSTOMER")
                .previousHash("GENESIS")
                .occurredAt(OffsetDateTime.now().withHour(hour))
                .build();
    }

    @Test
    @DisplayName("Sem regras ativas → nenhuma violação")
    void noActiveRulesProducesNoViolations() {
        when(ruleRepository.findAllByTenantIdAndEnabledTrue(tenantId)).thenReturn(List.of());

        List<RuleViolation> violations = ruleEngine.evaluate(buildEvent("LOGIN", 10));

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Regra TIME_WINDOW detecta acesso às 3h (janela 22h-6h)")
    void timeWindowRuleDetectsNightAccess() {
        DetectionRule rule = DetectionRule.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .name("No-hours access")
                .ruleType(DetectionRule.RuleType.TIME_WINDOW)
                .parameters(Map.of("startHour", 22, "endHour", 6))
                .riskScoreDelta(20)
                .enabled(true)
                .build();

        when(ruleRepository.findAllByTenantIdAndEnabledTrue(tenantId)).thenReturn(List.of(rule));

        List<RuleViolation> violations = ruleEngine.evaluate(buildEvent("LOGIN", 3));

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).title()).contains("horário atípico");
    }

    @Test
    @DisplayName("Regra TIME_WINDOW não dispara fora da janela proibida")
    void timeWindowRuleIgnoresDaytimeAccess() {
        DetectionRule rule = DetectionRule.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .name("No-hours access")
                .ruleType(DetectionRule.RuleType.TIME_WINDOW)
                .parameters(Map.of("startHour", 22, "endHour", 6))
                .riskScoreDelta(20)
                .enabled(true)
                .build();

        when(ruleRepository.findAllByTenantIdAndEnabledTrue(tenantId)).thenReturn(List.of(rule));

        List<RuleViolation> violations = ruleEngine.evaluate(buildEvent("LOGIN", 14));

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Regra THRESHOLD dispara quando ZCARD >= count")
    void thresholdRuleTriggersWhenLimitReached() {
        // Simula 60 eventos na janela (>= threshold de 50)
        when(redis.opsForZSet().zCard(anyString())).thenReturn(60L);

        DetectionRule rule = DetectionRule.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .name("Data export limit")
                .ruleType(DetectionRule.RuleType.THRESHOLD)
                .parameters(Map.of("action", "DATA_EXPORT", "count", 50, "windowMinutes", 60))
                .riskScoreDelta(30)
                .enabled(true)
                .build();

        when(ruleRepository.findAllByTenantIdAndEnabledTrue(tenantId)).thenReturn(List.of(rule));

        List<RuleViolation> violations = ruleEngine.evaluate(buildEvent("DATA_EXPORT", 10));

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).title()).contains("Threshold excedido");
    }

    @Test
    @DisplayName("Regra THRESHOLD ignora actions diferentes")
    void thresholdRuleIgnoresDifferentAction() {
        DetectionRule rule = DetectionRule.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .name("Data export limit")
                .ruleType(DetectionRule.RuleType.THRESHOLD)
                .parameters(Map.of("action", "DATA_EXPORT", "count", 50, "windowMinutes", 60))
                .riskScoreDelta(30)
                .enabled(true)
                .build();

        when(ruleRepository.findAllByTenantIdAndEnabledTrue(tenantId)).thenReturn(List.of(rule));

        // LOGIN não é DATA_EXPORT → regra não se aplica
        List<RuleViolation> violations = ruleEngine.evaluate(buildEvent("LOGIN", 10));

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Múltiplas regras → múltiplas violações independentes")
    void multipleRulesProduceMultipleViolations() {
        when(redis.opsForZSet().zCard(anyString())).thenReturn(300L); // viola HIGH_VOLUME

        DetectionRule timeRule = DetectionRule.builder()
                .id(UUID.randomUUID()).tenantId(tenantId).name("Night")
                .ruleType(DetectionRule.RuleType.TIME_WINDOW)
                .parameters(Map.of("startHour", 22, "endHour", 6))
                .riskScoreDelta(20).enabled(true).build();

        DetectionRule volumeRule = DetectionRule.builder()
                .id(UUID.randomUUID()).tenantId(tenantId).name("Volume")
                .ruleType(DetectionRule.RuleType.HIGH_VOLUME)
                .parameters(Map.of("maxEventsPerMinute", 200))
                .riskScoreDelta(40).enabled(true).build();

        when(ruleRepository.findAllByTenantIdAndEnabledTrue(tenantId))
                .thenReturn(List.of(timeRule, volumeRule));

        List<RuleViolation> violations = ruleEngine.evaluate(buildEvent("DATA_EXPORT", 3));

        assertThat(violations).hasSize(2);
    }


}
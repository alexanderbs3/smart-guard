package br.leetjourney.smartguard.domain.service.ruleengine;


import br.leetjourney.smartguard.domain.model.AuditEvent;
import br.leetjourney.smartguard.domain.model.DetectionRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThresholdRuleEvaluator implements RuleEvaluator {

    private final StringRedisTemplate redis;

    @Override
    public DetectionRule.RuleType supports() {
        return DetectionRule.RuleType.THRESHOLD;
    }

    @Override
    public Optional<RuleViolation> evaluate(AuditEvent event, DetectionRule rule) {
        Map<String, Object> params = rule.getParameters();

        String targetAction  = (String) params.get("action");
        int threshold        = toInt(params.get("count"), 50);
        int windowMinutes    = toInt(params.get("windowMinutes"), 60);
        long windowMs        = (long) windowMinutes * 60 * 1_000;

        // Só avalia se a action do evento bate com a da regra
        if (targetAction != null && !targetAction.equals(event.getAction())) {
            return Optional.empty();
        }

        String key = "threshold:%s:%s:%s".formatted(
                event.getTenantId(), event.getActorId(), event.getAction());
        long now = Instant.now().toEpochMilli();

        redis.opsForZSet().add(key, event.getId().toString(), now);
        redis.opsForZSet().removeRangeByScore(key, 0, now - windowMs);
        redis.expire(key, windowMinutes * 2L, TimeUnit.MINUTES);

        Long count = redis.opsForZSet().zCard(key);
        long current = count != null ? count : 0;

        if (current >= threshold) {
            return Optional.of(RuleViolation.of(
                    rule,
                    "Threshold excedido: %s".formatted(event.getAction()),
                    "%d ocorrências de '%s' em %d min (limite: %d)"
                            .formatted(current, event.getAction(), windowMinutes, threshold),
                    Map.of(
                            "action", event.getAction(),
                            "count", current,
                            "threshold", threshold,
                            "windowMinutes", windowMinutes,
                            "actorId", event.getActorId()
                    )
            ));
        }

        return Optional.empty();
    }

    private int toInt(Object value, int defaultVal) {
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) return Integer.parseInt(s);
        return defaultVal;
    }

}

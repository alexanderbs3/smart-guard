package br.leetjourney.smartguard.domain.service.ruleengine;


import br.leetjourney.smartguard.domain.model.AuditEvent;
import br.leetjourney.smartguard.domain.model.DetectionRule;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class HighVolumeRuleEvaluator implements RuleEvaluator {


    private final StringRedisTemplate redis;

    @Override
    public DetectionRule.RuleType supports() {
        return DetectionRule.RuleType.HIGH_VOLUME;
    }

    @Override
    public Optional<RuleViolation> evaluate(AuditEvent event, DetectionRule rule) {
        int maxPerMinute = toInt(rule.getParameters().get("maxEventsPerMinute"), 200);

        String key = "highvol:%s:%s".formatted(event.getTenantId(), event.getActorId());
        long now = Instant.now().toEpochMilli();
        long oneMinuteAgo = now - 60_000;

        redis.opsForZSet().add(key, event.getId().toString(), now);
        redis.opsForZSet().removeRangeByScore(key, 0, oneMinuteAgo);
        redis.expire(key, 5, TimeUnit.MINUTES);

        Long count = redis.opsForZSet().zCard(key);
        long current = count != null ? count : 0;

        if (current >= maxPerMinute) {
            return Optional.of(RuleViolation.of(
                    rule,
                    "Volume anômalo de eventos",
                    "%d eventos/min pelo ator '%s' (limite: %d)"
                            .formatted(current, event.getActorId(), maxPerMinute),
                    Map.of("eventsPerMinute", current, "limit", maxPerMinute,
                            "actorId", event.getActorId())
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

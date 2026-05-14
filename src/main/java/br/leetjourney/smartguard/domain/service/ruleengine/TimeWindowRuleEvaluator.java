package br.leetjourney.smartguard.domain.service.ruleengine;

import br.leetjourney.smartguard.domain.model.AuditEvent;
import br.leetjourney.smartguard.domain.model.DetectionRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class TimeWindowRuleEvaluator  implements RuleEvaluator {

    @Override
    public DetectionRule.RuleType supports() {
        return DetectionRule.RuleType.TIME_WINDOW;
    }

    @Override
    public Optional<RuleViolation> evaluate(AuditEvent event, DetectionRule rule) {
        Map<String, Object> params = rule.getParameters();

        String targetAction = (String) params.get("action");
        if (targetAction != null && !targetAction.equals(event.getAction())) {
            return Optional.empty();
        }

        int startHour = toInt(params.get("startHour"), 22);
        int endHour   = toInt(params.get("endHour"),   6);
        int eventHour = event.getOccurredAt().getHour();

        boolean inForbiddenWindow = isInWindow(eventHour, startHour, endHour);

        if (inForbiddenWindow) {
            return Optional.of(RuleViolation.of(
                    rule,
                    "Acesso em horário atípico",
                    "Ação '%s' pelo ator '%s' às %02dh (janela restrita: %02dh–%02dh)"
                            .formatted(event.getAction(), event.getActorId(),
                                    eventHour, startHour, endHour),
                    Map.of(
                            "eventHour", eventHour,
                            "startHour", startHour,
                            "endHour",   endHour,
                            "actorId",   event.getActorId(),
                            "action",    event.getAction()
                    )
            ));
        }

        return Optional.empty();
    }

    /** Suporta janelas que cruzam meia-noite (ex: 22h → 6h) */
    private boolean isInWindow(int hour, int start, int end) {
        if (start < end) {
            return hour >= start && hour < end;
        }
        // Janela noturna: start > end (ex: 22 → 6)
        return hour >= start || hour < end;
    }

    private int toInt(Object value, int defaultVal) {
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) return Integer.parseInt(s);
        return defaultVal;
    }

}

package br.leetjourney.smartguard.domain.service.ruleengine;

import br.leetjourney.smartguard.domain.model.DetectionRule;

import java.util.Map;

public record RuleViolation (
        DetectionRule rule,
        String title,
        String description,
        Map<String, Object> context
) {
    public static RuleViolation of(DetectionRule rule, String title,
                                   String description, Map<String, Object> ctx) {
        return new RuleViolation(rule, title, description, ctx);
    }
}


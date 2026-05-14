package br.leetjourney.smartguard.domain.service.ruleengine;

import br.leetjourney.smartguard.domain.model.AuditEvent;
import br.leetjourney.smartguard.domain.model.DetectionRule;

import java.util.Optional;

public interface RuleEvaluator {

    DetectionRule.RuleType supports();

    /**
     * Avalia se o evento viola a regra.
     * @return Optional.of(violation) se violou, Optional.empty() se não
     */
    Optional<RuleViolation> evaluate(AuditEvent event, DetectionRule rule);
}

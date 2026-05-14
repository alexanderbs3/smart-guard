package br.leetjourney.smartguard.domain.service.ruleengine;

import br.leetjourney.smartguard.domain.model.AuditEvent;
import br.leetjourney.smartguard.domain.model.DetectionRule;
import br.leetjourney.smartguard.domain.repository.DetectionRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RuleEngine {

    private final DetectionRuleRepository ruleRepository;
    private final Map<DetectionRule.RuleType, RuleEvaluator> evaluators;

    public RuleEngine(DetectionRuleRepository ruleRepository,
                      List<RuleEvaluator> evaluatorList) {
        this.ruleRepository = ruleRepository;
        this.evaluators = evaluatorList.stream()
                .collect(Collectors.toMap(RuleEvaluator::supports, Function.identity()));

        log.info("RuleEngine inicializado com {} evaluators: {}",
                evaluators.size(), evaluators.keySet());
    }

    /**
     * Avalia todas as regras ativas do tenant contra o evento.
     * Retorna a lista de violações encontradas.
     */
    public List<RuleViolation> evaluate(AuditEvent event) {
        List<DetectionRule> activeRules =
                ruleRepository.findAllByTenantIdAndEnabledTrue(event.getTenantId());

        if (activeRules.isEmpty()) return List.of();

        return activeRules.stream()
                .map(rule -> evaluateRule(event, rule))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Optional<RuleViolation> evaluateRule(AuditEvent event, DetectionRule rule) {
        RuleEvaluator evaluator = evaluators.get(rule.getRuleType());

        if (evaluator == null) {
            log.warn("Nenhum evaluator para o tipo: {}", rule.getRuleType());
            return Optional.empty();
        }

        try {
            return evaluator.evaluate(event, rule);
        } catch (Exception e) {
            log.error("Erro ao avaliar regra {} ({}): {}", rule.getId(), rule.getName(), e.getMessage());
            return Optional.empty();
        }
    }

}

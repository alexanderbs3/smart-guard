package br.leetjourney.smartguard.domain.repository;

import br.leetjourney.smartguard.domain.model.DetectionRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DetectionRuleRepository extends JpaRepository<DetectionRule, UUID> {

    List<DetectionRule> findAllByTenantIdAndEnabledTrue(UUID tenantId);
}

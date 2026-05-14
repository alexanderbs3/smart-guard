package br.leetjourney.smartguard.interfaces.rest;


import br.leetjourney.smartguard.domain.model.DetectionRule;
import br.leetjourney.smartguard.domain.model.User;
import br.leetjourney.smartguard.domain.repository.DetectionRuleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
@Tag(name = "Detection Rules", description = "Gerenciamento de regras de detecção por tenant")
@SecurityRequirement(name = "bearerAuth")
public class DetectionRuleController {

    private final DetectionRuleRepository ruleRepository;

    public record CreateRuleRequest(
            @NotBlank String name,
            String description,
            @NotNull DetectionRule.RuleType ruleType,
            Map<String, Object> parameters,
            Integer riskScoreDelta
    ) {}

    @GetMapping
    @Operation(summary = "Listar regras ativas do tenant")
    public ResponseEntity<List<DetectionRule>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                ruleRepository.findAllByTenantIdAndEnabledTrue(user.getTenant().getId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "Criar regra de detecção")
    public ResponseEntity<DetectionRule> create(
            @Valid @RequestBody CreateRuleRequest req,
            @AuthenticationPrincipal User user) {

        DetectionRule rule = ruleRepository.save(DetectionRule.builder()
                .tenantId(user.getTenant().getId())
                .name(req.name())
                .description(req.description())
                .ruleType(req.ruleType())
                .parameters(req.parameters() != null ? req.parameters() : Map.of())
                .riskScoreDelta(req.riskScoreDelta() != null ? req.riskScoreDelta() : 10)
                .enabled(true)
                .build());

        return ResponseEntity.status(HttpStatus.CREATED).body(rule);
    }

    @PatchMapping("/{ruleId}/toggle")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "Ativar / desativar regra")
    public ResponseEntity<DetectionRule> toggle(
            @PathVariable UUID ruleId,
            @AuthenticationPrincipal User user) {

        DetectionRule rule = ruleRepository.findById(ruleId)
                .filter(r -> r.getTenantId().equals(user.getTenant().getId()))
                .orElseThrow(() -> ResourceNotFoundException.of("DetectionRule", ruleId));

        rule.setEnabled(!rule.getEnabled());
        return ResponseEntity.ok(ruleRepository.save(rule));
    }

    @DeleteMapping("/{ruleId}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "Remover regra")
    public ResponseEntity<Void> delete(
            @PathVariable UUID ruleId,
            @AuthenticationPrincipal User user) {

        DetectionRule rule = ruleRepository.findById(ruleId)
                .filter(r -> r.getTenantId().equals(user.getTenant().getId()))
                .orElseThrow(() -> ResourceNotFoundException.of("DetectionRule", ruleId));

        ruleRepository.delete(rule);
        return ResponseEntity.noContent().build();
    }

}

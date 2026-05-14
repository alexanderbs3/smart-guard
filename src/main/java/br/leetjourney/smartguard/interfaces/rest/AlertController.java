package br.leetjourney.smartguard.interfaces.rest;

import br.leetjourney.smartguard.application.dto.AlertNotification;
import br.leetjourney.smartguard.domain.model.Alert;
import br.leetjourney.smartguard.domain.model.User;
import br.leetjourney.smartguard.domain.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Consulta e gestão de alertas de segurança")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @Operation(summary = "Listar alertas", description = "Retorna alertas do tenant. Filtre por status com ?status=OPEN")
    public ResponseEntity<Page<AlertNotification>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Alert.AlertStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(
                alertService.listByTenant(user.getTenant().getId(), status, pageable));
    }

    @PatchMapping("/{alertId}/acknowledge")
    @Operation(summary = "Reconhecer alerta", description = "Marca alerta como ACKNOWLEDGED")
    public ResponseEntity<AlertNotification> acknowledge(
            @PathVariable UUID alertId,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(alertService.acknowledge(alertId, user.getId()));
    }

    @PatchMapping("/{alertId}/resolve")
    @Operation(summary = "Resolver alerta", description = "Marca alerta como RESOLVED")
    public ResponseEntity<AlertNotification> resolve(
            @PathVariable UUID alertId,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(alertService.resolve(alertId, user.getId()));
    }

}

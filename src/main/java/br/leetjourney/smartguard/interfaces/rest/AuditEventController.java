package br.leetjourney.smartguard.interfaces.rest;

import br.leetjourney.smartguard.application.dto.AuditEventResponse;
import br.leetjourney.smartguard.application.dto.IngestEventRequest;
import br.leetjourney.smartguard.domain.model.User;
import br.leetjourney.smartguard.domain.service.IngestEventService;
import br.leetjourney.smartguard.domain.service.QueryEventService;
import br.leetjourney.smartguard.infrastructure.multitenancy.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Audit Events", description = "Ingestão e consulta de eventos de auditoria")
@SecurityRequirement(name = "bearerAuth")

public class AuditEventController {
    private final IngestEventService ingestEventService;
    private final QueryEventService queryEventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Ingerir evento",
            description = "Registra um evento de auditoria com hash chain e risk score automáticos."
    )
    public ResponseEntity<AuditEventResponse> ingest(
            @Valid @RequestBody IngestEventRequest request,
            @AuthenticationPrincipal User user) {

        UUID tenantId = user != null
                ? user.getTenant().getId()
                : TenantContext.getCurrentTenant();

        AuditEventResponse response = ingestEventService.ingest(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Listar eventos",
            description = "Retorna eventos do tenant autenticado com paginação. Aceita ?page=0&size=20."
    )
    public ResponseEntity<Page<AuditEventResponse>> list(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "occurredAt") Pageable pageable) {

        return ResponseEntity.ok(queryEventService.listByTenant(user.getTenant().getId(), pageable));
    }
}

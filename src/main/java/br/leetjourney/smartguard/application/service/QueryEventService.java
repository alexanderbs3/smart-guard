package br.leetjourney.smartguard.application.service;


import br.leetjourney.smartguard.application.dto.AuditEventResponse;
import br.leetjourney.smartguard.domain.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueryEventService {

    private final AuditEventRepository auditEventRepository;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'ANALYST', 'VIEWER')")
    public Page<AuditEventResponse> listByTenant(UUID tenantId, Pageable pageable) {
        return auditEventRepository
                .findAllByTenantIdOrderByOccurredAtDesc(tenantId, pageable)
                .map(AuditEventResponse::from);
    }
}

package br.leetjourney.smartguard.domain.repository;

import br.leetjourney.smartguard.domain.model.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, AuditEvent.AuditEventId>,
        JpaSpecificationExecutor<AuditEvent> {

    /**
     * Busca o hash do evento mais recente do tenant.
     * Usado pelo HashChainService para encadear o próximo evento.
     */
    @Query("""
        SELECT a.eventHash FROM AuditEvent a
        WHERE a.tenantId = :tenantId
        ORDER BY a.occurredAt DESC
        LIMIT 1
        """)
    Optional<String> findLatestHashByTenantId(UUID tenantId);

    /**
     * Verifica se já existe um evento com esse hash — garante idempotência.
     */
    boolean existsByEventHash(String eventHash);

    Page<AuditEvent> findAllByTenantIdOrderByOccurredAtDesc(UUID tenantId, Pageable pageable);

}

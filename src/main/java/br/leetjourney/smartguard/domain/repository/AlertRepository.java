package br.leetjourney.smartguard.domain.repository;

import br.leetjourney.smartguard.domain.model.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    Page<Alert> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    Page<Alert> findAllByTenantIdAndStatusOrderByCreatedAtDesc(
            UUID tenantId, Alert.AlertStatus status, Pageable pageable);

    long countByTenantIdAndStatus(UUID tenantId, Alert.AlertStatus status);
}

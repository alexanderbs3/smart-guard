package br.leetjourney.smartguard.domain.service;

import br.leetjourney.smartguard.domain.model.AuditEvent;
import br.leetjourney.smartguard.domain.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Serviço de integridade criptográfica do audit trail.
 *
 * Cada evento contém:
 *   event_hash    = SHA256(previousHash + tenantId + actorId + action + resourceType + resourceId + occurredAt)
 *   previous_hash = event_hash do evento anterior do mesmo tenant (ou "GENESIS" se for o primeiro)
 *
 * Isso forma uma cadeia imutável: qualquer adulteração em um evento invalida todos os seguintes.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class HashChainService {

    public static final String GENESIS_HASH = "GENESIS";

    private final AuditEventRepository auditEventRepository;

    /**
     * Retorna o hash do evento mais recente do tenant.
     * Se não existir nenhum, retorna GENESIS.
     */
    public String getLatestHash(UUID tenantId) {
        return auditEventRepository
                .findLatestHashByTenantId(tenantId)
                .orElse(GENESIS_HASH);
    }

    /**
     * Computa o SHA-256 do evento com base nos campos imutáveis + previousHash.
     * A concatenação garante que qualquer mudança em qualquer campo quebre a cadeia.
     */
    public String computeHash(String previousHash, AuditEvent event) {
        String payload = String.join("|",
                previousHash,
                event.getTenantId().toString(),
                event.getActorId(),
                event.getActorType().name(),
                event.getAction(),
                event.getResourceType(),
                event.getResourceId() != null ? event.getResourceId() : "",
                event.getOccurredAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        );

        return sha256(payload);
    }

    /**
     * Verifica a integridade de um evento isolado.
     * Para verificar a cadeia completa, use ChainVerificationService (Fase 3).
     */
    public boolean verifyEventHash(AuditEvent event, String previousHash) {
        String expected = computeHash(previousHash, event);
        return expected.equals(event.getEventHash());
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 é garantido pelo JDK — nunca ocorre em runtime
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

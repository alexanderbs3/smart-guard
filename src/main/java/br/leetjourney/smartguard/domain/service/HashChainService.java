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

@Slf4j
@Service
@RequiredArgsConstructor
public class HashChainService {

    public static final String GENESIS_HASH = "GENESIS";

    private final AuditEventRepository auditEventRepository;

    public String getLatestHash(UUID tenantId) {
        return auditEventRepository
                .findLatestHashByTenantId(tenantId)
                .orElse(GENESIS_HASH);
    }

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
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

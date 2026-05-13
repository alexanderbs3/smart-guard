package br.leetjourney.smartguard.domain.service;

import br.leetjourney.smartguard.domain.model.AuditEvent;
import br.leetjourney.smartguard.domain.repository.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HashChainServiceTest {

    @Mock
    AuditEventRepository auditEventRepository;
    @InjectMocks
     HashChainService hashChainService;

    private UUID tenantId;
    private AuditEvent event;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        event = AuditEvent.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .actorId("user-123")
                .actorType(AuditEvent.ActorType.USER)
                .action("DATA_EXPORT")
                .resourceType("CUSTOMER")
                .resourceId("cust-456")
                .previousHash(HashChainService.GENESIS_HASH)
                .occurredAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Primeiro evento do tenant usa GENESIS como previousHash")
    void firstEventUsesGenesisHash() {
        when(auditEventRepository.findLatestHashByTenantId(tenantId)).thenReturn(Optional.empty());
        assertThat(hashChainService.getLatestHash(tenantId)).isEqualTo(HashChainService.GENESIS_HASH);
    }

    @Test
    @DisplayName("Hash tem 64 caracteres (SHA-256 hex)")
    void hashIs64CharHex() {
        String hash = hashChainService.computeHash(HashChainService.GENESIS_HASH, event);
        assertThat(hash).hasSize(64).matches("[a-f0-9]+");
    }

    @Test
    @DisplayName("Hash é determinístico — mesmo evento produz mesmo hash")
    void hashIsDeterministic() {
        String h1 = hashChainService.computeHash(HashChainService.GENESIS_HASH, event);
        String h2 = hashChainService.computeHash(HashChainService.GENESIS_HASH, event);
        assertThat(h1).isEqualTo(h2);
    }

    @Test
    @DisplayName("Alterar actorId muda o hash — cadeia detecta adulteração")
    void differentActorProducesDifferentHash() {
        String hashA = hashChainService.computeHash(HashChainService.GENESIS_HASH, event);

        AuditEvent modified = AuditEvent.builder()
                .id(event.getId())
                .tenantId(tenantId)
                .actorId("OUTRO_ATOR")   // adulterado
                .actorType(AuditEvent.ActorType.USER)
                .action("DATA_EXPORT")
                .resourceType("CUSTOMER")
                .resourceId("cust-456")
                .previousHash(HashChainService.GENESIS_HASH)
                .occurredAt(event.getOccurredAt())
                .build();

        String hashB = hashChainService.computeHash(HashChainService.GENESIS_HASH, modified);
        assertThat(hashA).isNotEqualTo(hashB);
    }

    @Test
    @DisplayName("verifyEventHash valida o hash corretamente")
    void verifyEventHashReturnsTrueForValidEvent() {
        String hash = hashChainService.computeHash(HashChainService.GENESIS_HASH, event);

        AuditEvent withHash = AuditEvent.builder()
                .id(event.getId())
                .tenantId(tenantId)
                .actorId(event.getActorId())
                .actorType(event.getActorType())
                .action(event.getAction())
                .resourceType(event.getResourceType())
                .resourceId(event.getResourceId())
                .previousHash(HashChainService.GENESIS_HASH)
                .eventHash(hash)
                .occurredAt(event.getOccurredAt())
                .build();

        assertThat(hashChainService.verifyEventHash(withHash, HashChainService.GENESIS_HASH)).isTrue();
    }


}
package br.leetjourney.smartguard.infrastructure.messaging;


import br.leetjourney.smartguard.application.dto.event.IngestEventRequest;
import br.leetjourney.smartguard.application.service.IngestEventService;
import org.springframework.messaging.handler.annotation.Header;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventKafkaConsumer {
    public static final String TOPIC = "smartguard.audit-events";

    private final IngestEventService ingestEventService;

    @KafkaListener(
            topics = TOPIC,
            groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "3"   // 3 threads paralelas por partição
    )
    public void consume(
            @Payload KafkaEventMessage message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.debug("Kafka event recebido: topic={} partition={} offset={} tenant={}",
                TOPIC, partition, offset, message.tenantId());

        // Converte a mensagem Kafka para o mesmo DTO do REST
        IngestEventRequest request = new IngestEventRequest(
                message.actorId(),
                message.actorType(),
                message.sessionId(),
                message.action(),
                message.resourceType(),
                message.resourceId(),
                message.ipAddress(),
                message.userAgent(),
                message.metadata(),
                message.occurredAt()
        );

        ingestEventService.ingest(message.tenantId(), request);
    }
}

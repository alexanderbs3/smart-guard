package br.leetjourney.smartguard.infrastructure.config;

import br.leetjourney.smartguard.infrastructure.messaging.AuditEventKafkaConsumer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic auditEventsTopic() {
        return TopicBuilder.name(AuditEventKafkaConsumer.TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}

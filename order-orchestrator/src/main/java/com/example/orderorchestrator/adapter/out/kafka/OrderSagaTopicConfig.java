package com.example.orderorchestrator.adapter.out.kafka;

import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
@Profile("test")
public class OrderSagaTopicConfig {
    @Bean
    public KafkaAdmin.NewTopics orderSagaEventsTopic(
            @Value("${order.saga.events.topic:order-saga-events}") String topic
    ) {
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name(topic)
                        .config(TopicConfig.RETENTION_MS_CONFIG, "30000")
                        .build()
        );
    }
}

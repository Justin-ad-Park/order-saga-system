package com.example.orderorchestrator.adapter.out.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;

abstract class AbstractOrderSagaTopicDelete {
    @Value("${spring.kafka.bootstrap-servers}")
    protected String bootstrapServers;

    protected abstract String topic();

    @Test
    void deleteTopicIfExists() throws Exception {
        KafkaTopicRetentionReader.deleteTopicIfExists(bootstrapServers, topic());
        System.out.println("Deleted topic if existed: " + topic());
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration(KafkaAutoConfiguration.class)
    static class KafkaTestConfig {
    }
}

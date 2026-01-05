package com.example.orderorchestrator.adapter.out.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.kafka.common.config.TopicConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(
        classes = OrderSagaTopicRetentionIntegrationTest.KafkaTestConfig.class,
        properties = {
                "spring.config.name=orderOS_application",
                "spring.profiles.active=test"
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class OrderSagaTopicRetentionIntegrationTest {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${order.saga.events.topic:order-saga-events}")
    private String topic;

    @Test
    void retentionMs_shouldMatchTestConfig() throws Exception {
        String retentionMs = KafkaTopicRetentionReader.readRetentionMs(bootstrapServers, topic);
        System.out.println("Kafka retention.ms for " + topic + " = " + retentionMs);
        assertThat(retentionMs).isEqualTo("30000");
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration(KafkaAutoConfiguration.class)
    @Import(OrderSagaTopicConfig.class)
    static class KafkaTestConfig {
    }
}

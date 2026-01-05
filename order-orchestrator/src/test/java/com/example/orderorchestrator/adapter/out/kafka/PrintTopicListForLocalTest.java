package com.example.orderorchestrator.adapter.out.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = PrintTopicListForLocalTest.KafkaTestConfig.class,
        properties = {
                "spring.config.name=orderOS_application",
                "spring.profiles.active=test",
                "logging.level.root=OFF"
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class PrintTopicListForLocalTest {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${order.saga.events.topic:order-saga-events-test}")
    private String topic;

    @Test
    void printPublishedTopics() {
        KafkaTopicPrinter.printKafkaTopics(bootstrapServers, topic);
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration(KafkaAutoConfiguration.class)
    static class KafkaTestConfig {
    }
}

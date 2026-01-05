package com.example.orderorchestrator.adapter.out.kafka;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = OrderSagaTopicListIntegrationTest.KafkaTestConfig.class,
        properties = {
                "spring.config.name=orderOS_application",
                "spring.profiles.active=test"
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class OrderSagaTopicListIntegrationTest {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${order.saga.events.topic:order-saga-events-test}")
    private String topic;

    @Test
    void printPublishedTopics() throws Exception {
        try (AdminClient adminClient = AdminClient.create(
                Map.of(
                        AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "2000",
                        AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "2000"
                ))) {
            Set<String> topics = adminClient.listTopics().names().get(5, TimeUnit.SECONDS);
            Set<String> matched = topics.contains(topic) ? Set.of(topic) : Set.of();
            System.out.println("Published topics for " + topic + " = " + matched);
        }
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration(KafkaAutoConfiguration.class)
    static class KafkaTestConfig {
    }
}

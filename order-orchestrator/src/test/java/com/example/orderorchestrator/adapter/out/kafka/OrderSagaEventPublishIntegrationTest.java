package com.example.orderorchestrator.adapter.out.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@SpringBootTest(
        classes = OrderSagaEventPublishIntegrationTest.KafkaTestConfig.class,
        properties = {
                "spring.config.name=orderOS_application",
                "spring.profiles.active=test"
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class OrderSagaEventPublishIntegrationTest {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${order.saga.events.topic:order-saga-events-test}")
    private String topic;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Test
    void publishToTestTopic_shouldSucceed() throws Exception {
        assertKafkaAvailable();

        String key = "TEST-" + UUID.randomUUID();
        String payload = "{\"ping\":\"" + UUID.randomUUID() + "\"}";

        SendResult<String, String> result = kafkaTemplate
                .send(topic, key, payload)
                .get(10, TimeUnit.SECONDS);

        RecordMetadata metadata = result.getRecordMetadata();
        assertThat(metadata).isNotNull();
        assertThat(metadata.topic()).isEqualTo(topic);
    }

    private void assertKafkaAvailable() throws Exception {
        try (AdminClient adminClient = AdminClient.create(
                java.util.Map.of(
                        AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "2000",
                        AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "2000"
                ))) {
            adminClient.describeCluster().nodes().get(2, TimeUnit.SECONDS);
        } catch (Exception ex) {
            Assertions.fail("Kafka broker is not reachable at " + bootstrapServers, ex);
        }
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration(KafkaAutoConfiguration.class)
    static class KafkaTestConfig {
    }
}

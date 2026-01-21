package com.example.orderorchestrator.adapter.out.kafka;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
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
                        .config(TopicConfig.RETENTION_MS_CONFIG, "30000")   // 30초가 지나면 세그먼트(토픽이 물리적으로 저장되는 단위. 1세그먼트에 여러 토픽이 관리됨) 삭제됨
                        .build()
        );
    }

    @Bean
    public ApplicationRunner recreateTestTopicWithConfig(
            KafkaAdmin kafkaAdmin,
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${order.saga.events.topic:order-saga-events}") String topic
    ) {
        return args -> {
            // deleteTopicIfExists(bootstrapServers, topic);
            kafkaAdmin.initialize();
        };
    }

//    private void deleteTopicIfExists(String bootstrapServers, String topic) throws Exception {
//        try (AdminClient adminClient = AdminClient.create(
//                Map.of(
//                        AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
//                        AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "2000",
//                        AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "2000"
//                ))) {
//            Set<String> topics = adminClient.listTopics().names().get(5, TimeUnit.SECONDS);
//            if (!topics.contains(topic)) {
//                return;
//            }
//            adminClient.deleteTopics(List.of(topic)).all().get(5, TimeUnit.SECONDS);
//            waitUntilTopicDeleted(adminClient, topic);
//        }
//    }
//
//    private void waitUntilTopicDeleted(AdminClient adminClient, String topic) throws Exception {
//        long deadline = System.currentTimeMillis() + 10000L;
//        while (System.currentTimeMillis() < deadline) {
//            Set<String> topics = adminClient.listTopics().names().get(5, TimeUnit.SECONDS);
//            if (!topics.contains(topic)) {
//                return;
//            }
//            Thread.sleep(200L);
//        }
//        throw new IllegalStateException("Topic deletion timeout: " + topic);
//    }
}

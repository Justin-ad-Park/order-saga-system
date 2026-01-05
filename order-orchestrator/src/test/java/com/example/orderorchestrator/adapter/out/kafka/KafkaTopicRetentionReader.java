package com.example.orderorchestrator.adapter.out.kafka;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;

final class KafkaTopicRetentionReader {
    private KafkaTopicRetentionReader() {
    }

    static String readRetentionMs(String bootstrapServers, String topic) throws Exception {
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topic);
        try (AdminClient adminClient = AdminClient.create(
                Map.of(
                        AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "2000",
                        AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "2000"
                ))) {
            Config config = adminClient
                    .describeConfigs(List.of(resource))
                    .all()
                    .get(5, TimeUnit.SECONDS)
                    .get(resource);
            return config.get(TopicConfig.RETENTION_MS_CONFIG).value();
        }
    }

    static void deleteTopicIfExists(String bootstrapServers, String topic) throws Exception {
        try (AdminClient adminClient = AdminClient.create(
                Map.of(
                        AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "2000",
                        AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "2000"
                ))) {
            Set<String> topics = adminClient.listTopics().names().get(5, TimeUnit.SECONDS);
            if (!topics.contains(topic)) {
                return;
            }
            adminClient.deleteTopics(List.of(topic)).all().get(5, TimeUnit.SECONDS);
            waitUntilTopicDeleted(adminClient, topic);
        }
    }

    private static void waitUntilTopicDeleted(AdminClient adminClient, String topic) throws Exception {
        long deadline = System.currentTimeMillis() + 10000L;
        while (System.currentTimeMillis() < deadline) {
            Set<String> topics = adminClient.listTopics().names().get(5, TimeUnit.SECONDS);
            if (!topics.contains(topic)) {
                return;
            }
            Thread.sleep(200L);
        }
        throw new IllegalStateException("Topic deletion timeout: " + topic);
    }
}

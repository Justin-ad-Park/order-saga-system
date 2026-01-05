package com.example.orderorchestrator.adapter.out.kafka;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

public final class KafkaTopicPrinter {
    private KafkaTopicPrinter() {
    }

    public static void printKafkaTopics(String bootstrapServers, String topicFilter) {
        try (AdminClient adminClient = AdminClient.create(
                Map.of(
                        AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "2000",
                        AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "2000"
                ))) {
            Set<String> topics = adminClient.listTopics().names().get(2, TimeUnit.SECONDS);
            Set<String> selected = selectTopics(topics, topicFilter);
            System.out.println("\n\n##### Kafka topics ##### : " + selected);
            printKafkaPayloads(bootstrapServers, selected);
        } catch (Exception ex) {
            System.out.println("\n\n### Kafka topics 조회 실패 ### : " + ex.getMessage());
        }
    }

    private static Set<String> selectTopics(Set<String> topics, String topicFilter) {
        if (topicFilter == null || topicFilter.isBlank()) {
            return topics;
        }
        return topics.contains(topicFilter) ? Set.of(topicFilter) : Collections.emptySet();
    }

    private static void printKafkaPayloads(String bootstrapServers, Set<String> topics) {
        for (String topic : topics) {
            if (topic.startsWith("__")) {
                continue;
            }
            try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(
                    Map.of(
                            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                            ConsumerConfig.GROUP_ID_CONFIG, "order-orch-test-" + UUID.randomUUID(),
                            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false",
                            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
                            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()
                    ))) {
                consumer.subscribe(Set.of(topic));
                consumer.poll(Duration.ofMillis(500));
                consumer.seekToBeginning(consumer.assignment());

                System.out.println("\n\n#################");

                var records = consumer.poll(Duration.ofSeconds(2));
                if (records.isEmpty()) {
                    System.out.println("### Kafka payloads ### : " + topic + " (no records)");
                    continue;
                }
                records.forEach(record -> System.out.println(
                        "### Kafka payloads ### : " + record.topic()
                                + " partition=" + record.partition()
                                + " offset=" + record.offset()
                                + " key=" + record.key()
                                + " value=" + record.value()
                ));
            } catch (Exception ex) {
                System.out.println("### Kafka payloads 조회 실패 ### : " + topic + " message=" + ex.getMessage());
            }
        }
    }
}

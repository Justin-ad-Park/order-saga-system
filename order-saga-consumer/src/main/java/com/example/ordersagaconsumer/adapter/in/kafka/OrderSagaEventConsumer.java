package com.example.ordersagaconsumer.adapter.in.kafka;

import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderSagaEventConsumer {

    @KafkaListener(
            topics = "${order.saga.events.topic}",
            groupId = "${order.saga.events.consumer-group:order-saga-consumer}",
            containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void consume(List<ConsumerRecord<String, String>> records) {
        records.forEach(record -> System.out.println(
                "### Kafka payloads ### : " + record.topic()
                        + " partition=" + record.partition()
                        + " offset=" + record.offset()
                        + " key=" + record.key()
                        + " value=" + record.value()
        ));
    }
}

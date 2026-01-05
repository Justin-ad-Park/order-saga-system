package com.example.ordersagaconsumer.adapter.in.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import com.example.ordersagaconsumer.application.port.in.ProcessOrderSagaEventUseCase;
import com.example.ordersagaconsumer.adapter.in.kafka.dto.OrderSagaEventPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderSagaEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessOrderSagaEventUseCase processOrderSagaEventUseCase;

    public OrderSagaEventConsumer(
            ObjectMapper objectMapper,
            ProcessOrderSagaEventUseCase processOrderSagaEventUseCase
    ) {
        this.objectMapper = objectMapper;
        this.processOrderSagaEventUseCase = processOrderSagaEventUseCase;
    }

    @KafkaListener(
            topics = "${order.saga.events.topic}",
            groupId = "${order.saga.events.consumer-group:order-saga-consumer}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        OrderSagaEventPayload payload = readPayload(record.value());
        if (payload == null) {
            return;
        }
        processOrderSagaEventUseCase.process(payload.orderId(), payload.status());
    }

    private OrderSagaEventPayload readPayload(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, OrderSagaEventPayload.class);
        } catch (Exception ex) {
            System.out.println("### Kafka payload parse failed ### : message=" + ex.getMessage()
                    + " payload=" + rawPayload);
            return null;
        }
    }
}

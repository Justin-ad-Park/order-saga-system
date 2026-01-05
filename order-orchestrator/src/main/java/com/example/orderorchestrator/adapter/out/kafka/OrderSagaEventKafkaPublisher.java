package com.example.orderorchestrator.adapter.out.kafka;

import com.example.orderorchestrator.application.port.out.OrderSagaEventPublisher;
import com.example.orderorchestrator.domain.event.OrderSagaEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderSagaEventKafkaPublisher implements OrderSagaEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(OrderSagaEventKafkaPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public OrderSagaEventKafkaPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${order.saga.events.topic:order-saga-events}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publish(OrderSagaEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, event.orderId(), payload);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize OrderSagaEvent: orderId={}", event.orderId(), ex);
        }
    }
}

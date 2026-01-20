# 04. Kafka event publish and consume entry

## Goal
Show how saga events are published and how the consumer receives them.

## Core flow
- Orchestrator publishes event after reservation outcome
- Consumer reads the event from Kafka

## Event publish
`order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/kafka/OrderSagaEventKafkaPublisher.java`
```java
@Component
public class OrderSagaEventKafkaPublisher implements OrderSagaEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(OrderSagaEventKafkaPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

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
```

## Event consume entry
`order-saga-consumer/src/main/java/com/example/ordersagaconsumer/adapter/in/kafka/OrderSagaEventConsumer.java`
```java
@Component
public class OrderSagaEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessOrderSagaEventUseCase processOrderSagaEventUseCase;

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
}
```

## Hands-on checkpoints
- Kafka topic: `order-saga-events`
- Verify producer log + consumer receive log

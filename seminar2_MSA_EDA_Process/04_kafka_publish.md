# 04. Kafka 이벤트 발행과 소비 진입

## 목표
사가 이벤트가 어떻게 발행되고, 컨슈머가 이를 어떻게 수신하는지 이해한다.

## 핵심 흐름
- 오케스트레이터가 예약 결과에 따라 이벤트 발행  
- 컨슈머가 Kafka 이벤트를 수신

## 이벤트 발행
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

## 이벤트 소비 진입점
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

## 실습 체크포인트
- Kafka 토픽: `order-saga-events`
- 프로듀서/컨슈머 로그 확인

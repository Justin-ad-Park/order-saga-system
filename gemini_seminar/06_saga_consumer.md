# Chapter 6: Saga Consumer로 이벤트 소비(Consume)하기

`order-orchestrator`가 발행한 '주문 시작' 이벤트를 받아 실질적인 액션을 수행할 주체가 필요합니다. 본 챕터에서는 이 역할을 담당하는 새로운 마이크로서비스, `order-saga-consumer`의 탄생과 구현에 대해 알아봅니다.

## 1. 왜 별도의 Consumer 서비스인가?

Kafka 이벤트를 소비하는 로직을 `coupon-service`나 `point-service`가 직접 가질 수도 있습니다. 하지만 `order-saga-consumer`라는 별도의 전담 서비스를 만든 이유는 다음과 같습니다.

*   **Saga 로직 중앙화:** 여러 서비스에 걸친 분산 트랜잭션(Saga)의 흐름을 제어하는 로직이 여러 서비스에 흩어져 있으면 관리하기가 매우 복잡해집니다. `order-saga-consumer`는 '주문'이라는 Saga 시나리오에 대한 모든 처리 과정을 책임지는 중앙 제어실 역할을 합니다.
*   **관심사 분리:** `coupon-service`와 `point-service`는 오직 '쿠폰 차감', '포인트 적립'이라는 자신의 핵심 책임에만 집중할 수 있습니다.
*   **유연성:** 만약 새로운 서비스(예: `inventory-service`)가 Saga에 참여해야 할 경우, 다른 서비스의 변경 없이 `order-saga-consumer`의 로직만 수정하여 대응할 수 있습니다.

`order-saga-consumer`는 `order-orchestrator`가 오케스트레이션의 '시작'을 알리면, 그 뒤를 이어받아 각 참여 서비스(Participant)들을 조율하는 **Saga 코디네이터(Saga Coordinator)** 역할을 수행하는 것입니다.

## 2. 주요 Git 이력

아래 커밋들은 `order-saga-consumer` 서비스를 생성하고, 이벤트를 수신하여 다른 서비스와 상호작용하는 로직을 구현하는 과정을 보여줍니다.
```
* 0b73be2 | 2026-01-06 | ### Saga 컨슈머 confirm, compensate 로직 추가 ###
* 3afbfb9 | 2026-01-05 | Comsumer 기본 프로젝트 및 기본 로직 구성
* 66c93ca | 2026-01-06 | confirm, compansate API를 point-service에도 동일한 방식으로 추가
* 091c2a7 | 2026-01-05 | coupon-service에 보상(compensateCoupon) API 추가
* 542ed97 | 2026-01-05 | ### Coupon-service confirm API 추가 ###
```

## 3. 핵심 코드 스니펫

### Kafka 이벤트 수신 어댑터

`order-saga-consumer`의 `OrderSagaEventConsumer` 클래스는 `@KafkaListener` 어노테이션을 사용하여 Kafka 토픽을 구독하고, 이벤트가 발생했을 때 이를 수신하는 Input Adapter 역할을 합니다.

**`order-saga-consumer/.../in/kafka/OrderSagaEventConsumer.java`**
```java
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

    // ✅ application.yml 에 정의된 토픽과 컨슈머 그룹을 사용하여 이벤트를 리스닝
    @KafkaListener(
            topics = "${order.saga.events.topic}",
            groupId = "${order.saga.events.consumer-group:order-saga-consumer}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        // 1. 수신한 JSON 메시지를 자바 객체(Payload)로 변환
        OrderSagaEventPayload payload = readPayload(record.value());
        if (payload == null) {
            return;
        }
        // 2. 실제 비즈니스 로직 처리는 Application 계층의 UseCase 에 위임
        processOrderSagaEventUseCase.process(payload.orderId(), payload.status());
    }

    private OrderSagaEventPayload readPayload(String rawPayload) {
        // ... (JSON 파싱 로직)
    }
}
```
`consume` 메서드는 Kafka로부터 메시지를 받는 즉시, 복잡한 비즈니스 로직을 직접 처리하지 않고 `ProcessOrderSagaEventUseCase`라는 Port를 통해 Application 계층으로 작업을 위임합니다. 이는 Hexagonal Architecture의 원칙을 충실히 따르는 좋은 예시입니다. `UseCase`는 `coupon-service`와 `point-service`를 호출하는 등의 실제 Saga 조정 로직을 수행하게 됩니다.

---
이제 '주문 시작' 이벤트에 반응하여 쿠폰과 포인트를 처리하고, 그 결과에 따라 다음 행동(확정 또는 보상)을 결정하는 똑똑한 Consumer가 탄생했습니다. 하지만 아직 '보상' 시나리오가 완벽하게 구현되지는 않았습니다. 다음 챕터에서는 분산 트랜잭션의 꽃이라 할 수 있는 **Saga 보상 트랜잭션(Compensating Transaction)**을 구체적으로 어떻게 구현하는지 살펴보겠습니다.
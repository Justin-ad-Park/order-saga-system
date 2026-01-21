# Chapter 5: Kafka에 이벤트 발행(Publish)하기

Kafka라는 이벤트 백본이 마련되었으니, 이제 실제로 이벤트를 만들어 보내는 방법을 구현할 차례입니다. 본 챕터에서는 `order-orchestrator`가 어떻게 이벤트를 Kafka로 발행하는지 상세히 알아봅니다.

## 1. 이벤트 발행 아키텍처: 컨트롤러에서의 직접 발행

일반적인 "Polling Publisher" 방식의 Outbox Pattern은 별도의 스케줄러가 주기적으로 Outbox 테이블을 읽어 이벤트를 발행합니다.

하지만 **이 프로젝트에서는 `OUTBOX_MESSAGE` 테이블을 폴링하여 이벤트를 발행하는 전용 로직은 아직 구현되지 않았습니다.** 대신, `OrderOrchestrationController`가 전체 Saga 흐름의 성공/실패를 확인한 후, **컨트롤러의 로직 마지막 단계에서 직접 Kafka로 이벤트를 발행**합니다.

1.  **주문 생성 및 Outbox 저장:** `CreateOrderUseCase`가 호출되어 `OrderSaga`와 `OutboxMessage`가 같은 트랜잭션 안에서 원자적으로 저장됩니다. (Chapter 3 참조)
    *   _이때 `OUTBOX_MESSAGE`는 Saga의 현재 상태를 신뢰성 있게 기록하는 역할을 합니다. 향후 전용 폴링 퍼블리셔가 구현된다면 이 테이블이 이벤트 발행의 원천이 될 수 있습니다._
2.  **외부 서비스 호출:** `ReserveExternalResourcesService`가 `coupon-service`와 `point-service`를 비동기적으로 호출하여 리소스를 예약합니다.
3.  **결과 처리 및 이벤트 발행:**
    *   모든 외부 서비스 호출이 성공하면(`then` 블록), Saga의 상태를 `Reserved`로 업데이트하고, "예약 성공" 이벤트를 Kafka에 발행합니다.
    *   하나라도 실패하면(`onErrorResume` 블록), Saga의 상태를 `Compensating`으로 업데이트하고, "예약 실패" 이벤트를 Kafka에 발행합니다.

이 방식은 별도의 스케줄러 없이 컨트롤러가 전체 흐름을 명확하게 제어하는 장점이 있습니다. 그러나 이벤트 발행 로직이 컨트롤러의 주 요청/응답 사이클에 포함되어, 요청 처리 시간이 길어지거나 컨트롤러의 책임이 증가하는 단점도 존재합니다. `OUTBOX_MESSAGE` 테이블은 이 과정에서 Saga의 상태를 추적하고 기록하는 중요한 역할을 계속 수행합니다.

## 2. 주요 Git 이력

아래는 이벤트 발행 로직과 관련된 주요 커밋 내역입니다.
```
* aeceecc | 2026-01-05 | 테스트 토픽 분리 & 토픽 발행 테스트 OrderSagaEventPublishIntegrationTest
* 9a613a8 | 2025-12-31 | 토픽 생성, 기존 토픽 삭제, 이벤트(토픽) 발행, 브로커 접속 테스트, 이벤트 소비 테스트 추가
```

## 3. 핵심 코드 스니펫

### 컨트롤러에서의 이벤트 발행 로직

`OrderOrchestrationController`의 `createOrder` 메서드는 WebFlux의 리액티브 체이닝을 사용하여 전체 오케스트레이션과 이벤트 발행을 한번에 처리합니다.

**`order-orchestrator/.../in/web/OrderOrchestrationController.java`**
```java
@PostMapping
public Mono<ResponseEntity<CreateOrderResponse>> createOrder(
        @Valid @RequestBody CreateOrderRequest request
) {
    // 1. 주문 생성 및 Outbox 메시지 DB 저장 (Outbox는 Saga 상태 기록 용도)
    CreateOrderCommand command = mapToCommand(request);
    CreateOrderResult result = createOrderUseCase.createOrder(command);

    // 2. 외부 서비스(쿠폰, 포인트) 호출
    return reserveExternalResourcesService.reserveExternalResources(
                    result.orderId(),
                    request.couponNumber(),
                    request.pointNumber()
                )
            // 3a. 성공 시 Saga 상태 업데이트 및 이벤트 발행
            .then(Mono.fromRunnable(() -> {
                updateSagaStatus(result.orderId(), OrderSagaStatus.Reserved);
                publishSagaEvent(result, OrderSagaStatus.Reserved, OrderSagaEventType.RESERVE_SUCCEEDED);
            }))
            // 3b. 실패 시 Saga 상태 업데이트 및 이벤트 발행
            .onErrorResume(ex -> {
                updateSagaStatus(result.orderId(), OrderSagaStatus.Compensating);
                publishSagaEvent(result, OrderSagaStatus.Compensating, OrderSagaEventType.RESERVE_FAILED);
                return Mono.error(ex);
            })
            .thenReturn(ResponseEntity.ok(mapToResponse(result)));
}

// 👇 이벤트 발행을 담당하는 private 메서드
private void publishSagaEvent(CreateOrderResult result, OrderSagaStatus status, OrderSagaEventType type) {
    orderSagaEventService.publish(result.orderId(), result.sagaId(), status, type);
}
```

### Kafka 이벤트 발행 어댑터

`orderSagaEventService`는 내부적으로 `OrderSagaEventPublisher` 인터페이스에 의존하며, 그 실제 구현체인 `OrderSagaEventKafkaPublisher`가 `KafkaTemplate`을 사용하여 이벤트를 발행합니다.

**`order-orchestrator/.../out/kafka/OrderSagaEventKafkaPublisher.java`**
```java
@Component
public class OrderSagaEventKafkaPublisher implements OrderSagaEventPublisher {
    // ...
    private final KafkaTemplate<String, String> kafkaTemplate;

    // ...

    @Override
    public void publish(OrderSagaEvent event) {
        // ... (이벤트 객체를 JSON 문자열로 변환)
        String payload = toJson(event);
        log.info("Publishing event to Kafka: {}", payload);

        // KafkaTemplate 을 사용해 실제 이벤트 발행
        kafkaTemplate.send(topic, event.orderId(), payload);
    }
}
```

---
향후 개선 방향으로는 `OUTBOX_MESSAGE` 테이블을 폴링하여 이벤트를 발행하는 전용 마이크로서비스(예: Change Data Capture 기반의 Debezium)를 도입함으로써 `OrderOrchestrationController`의 책임을 더욱 분리하고 이벤트 발행의 신뢰성을 더욱 높일 수 있습니다.

이제 `order-orchestrator`는 외부 서비스 호출 결과에 따라 Saga의 다음 단계를 결정하고, 그 결과를 이벤트로 안정적으로 외부에 알릴 수 있게 되었습니다. 다음 챕터에서는 이 이벤트를 수신하여 실질적인 쿠폰 및 포인트 처리 작업을 수행할 **Saga Consumer**를 구현하는 과정을 살펴보겠습니다.

# Chapter 6: Saga Consumer로 이벤트 소비(Consume)하기

## 1. 개요: Saga의 다음 단계 - 이벤트 소비 및 처리

이전 챕터에서 `order-orchestrator`가 Saga 상태 변경 이벤트를 Kafka 토픽으로 성공적으로 발행하는 방법을 배웠습니다. 이제 발행된 이벤트는 누군가가 소비하여 Saga의 다음 단계를 진행해야 합니다. 본 챕터에서는 Kafka에 발행된 Saga 이벤트를 수신하고, 이를 기반으로 실질적인 비즈니스 로직을 처리하는 `order-saga-consumer` 서비스를 구현하는 방법을 알아봅니다. `order-saga-consumer`는 Saga 패턴에서 각 참여자 서비스들의 확정(confirm) 또는 보상(compensate) 로직을 트리거하는 중요한 역할을 수행합니다.

### 핵심 학습 목표
*   `order-saga-consumer`의 역할과 Saga 패턴에서의 중요성을 이해합니다.
*   Spring Kafka의 `@KafkaListener`를 사용하여 Kafka 이벤트를 소비하는 방법을 학습합니다.
*   소비된 이벤트를 기반으로 Saga의 상태를 파악하고 적절한 비즈니스 로직을 위임하는 컨슈머의 처리 흐름을 이해합니다.

## 2. `order-saga-consumer`의 역할

`order-saga-consumer`는 Kafka 토픽(`order-saga-events`)을 구독하여 `OrderSagaEvent`를 수신합니다. 수신된 이벤트의 `sagaStatus` (예: `Reserved`, `Compensating`)에 따라 적절한 서비스(`coupon-service`, `point-service`)에 확정(confirm) 또는 보상(compensate) 요청을 보내 Saga의 분산 트랜잭션을 진행하거나 되돌리는 역할을 수행합니다.

이 서비스는 Saga 패턴에서 "오케스트레이터" 역할을 하는 `order-orchestrator`와는 다른, 이벤트를 받아 실질적인 작업을 수행하는 "참여자(Participant)"의 성격을 띠지만, 동시에 다른 참여자 서비스의 최종 상태를 결정하는 중간 코디네이터 역할도 겸하고 있습니다.

## 3. `order-saga-consumer` 관련 Git 이력

`order-saga-consumer` 서비스의 초기 구현 및 Kafka 이벤트 소비 로직과 관련된 주요 Git 커밋입니다.

| 커밋 ID | 날짜 | 주요 변경 요약 |
|---|---|---|
| `3afbfb9` | 2026-01-05 | `order-saga-consumer` 기본 프로젝트 및 기본 로직 구성 |
| `0b73be2` | 2026-01-05 | Saga 컨슈머 `confirm`, `compensate` 로직 추가 |
| `a1f74d8` | 2026-01-05 | 컨슈머 호스트 테스트 추가 |
| `576a868` | 2026-01-06 | 컨슈머 실행 시 프로필 설정 오류 수정 |
| `5a250f8` | 2026-01-06 | Saga Local & K8s + Host Consumer 테스트 완료 |

**(실습 가이드: Git 커밋 확인)**
1.  `git checkout 3afbfb9` 명령어로 해당 커밋 시점으로 이동하여 `order-saga-consumer` 프로젝트의 초기 구조를 확인해 보세요.
2.  `git diff 3afbfb9~1 0b73be2` 명령어로 `confirm`/`compensate` 로직이 추가된 변경사항을 확인할 수 있습니다.

## 4. 핵심 코드 스니펫: Kafka 이벤트 소비

### 4.1. `OrderSagaEventConsumer` (Driving Adapter)

`OrderSagaEventConsumer`는 Spring Kafka의 `@KafkaListener` 애노테이션을 사용하여 `order-saga-events` 토픽으로부터 이벤트를 수신합니다. 수신된 메시지는 `ProcessOrderSagaEventUseCase` (Input Port)로 위임되어 비즈니스 로직을 처리합니다.

**`order-saga-consumer/src/main/java/com/example/ordersagaconsumer/adapter/in/kafka/OrderSagaEventConsumer.java`**
```java
// ... imports ...
@Component
public class OrderSagaEventConsumer { // Kafka 이벤트 소비를 위한 Driving Adapter

    private final ObjectMapper objectMapper; // JSON 파싱을 위한 ObjectMapper
    private final ProcessOrderSagaEventUseCase processOrderSagaEventUseCase; // Input Port

    public OrderSagaEventConsumer(
            ObjectMapper objectMapper,
            ProcessOrderSagaEventUseCase processOrderSagaEventUseCase
    ) {
        this.objectMapper = objectMapper;
        this.processOrderSagaEventUseCase = processOrderSagaEventUseCase;
    }

    @KafkaListener(
            topics = "${order.saga.events.topic}", // application.yml 에서 설정된 토픽 구독
            groupId = "${order.saga.events.consumer-group:order-saga-consumer}" // 컨슈머 그룹 ID
    )
    public void consume(ConsumerRecord<String, String> record) {
        System.out.println("### Kafka Event Consumed ### : " + record.value()); // 소비된 이벤트 로그 출력

        // 수신된 JSON 메시지 페이로드를 OrderSagaEventPayload 객체로 파싱
        OrderSagaEventPayload payload = readPayload(record.value());
        if (payload == null) { // 파싱 실패 시 처리 중단
            return;
        }

        // 파싱된 페이로드와 함께 UseCase 호출하여 비즈니스 로직 처리
        processOrderSagaEventUseCase.process(payload.orderId(), payload.status());
    }

    // JSON 페이로드 파싱 헬퍼 메서드
    private OrderSagaEventPayload readPayload(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, OrderSagaEventPayload.class);
        } catch (Exception ex) {
            System.err.println("### Kafka payload parse failed ### : message=" + ex.getMessage()
                    + " payload=" + rawPayload);
            return null;
        }
    }
}
```
**설명:** `@KafkaListener`는 지정된 토픽의 메시지를 자동으로 소비하도록 설정합니다. `consume` 메서드에서는 수신된 JSON 문자열을 `OrderSagaEventPayload` 객체로 역직렬화한 후, `processOrderSagaEventUseCase.process()`를 호출하여 실제 Saga 처리 로직을 시작합니다.

### 4.2. `ProcessOrderSagaEventService` (Application Layer)

`ProcessOrderSagaEventService`는 `ProcessOrderSagaEventUseCase` (Input Port)를 구현하며, Saga 이벤트의 `sagaStatus`에 따라 확정(`handleConfirm`) 또는 보상(`handleCompensate`) 로직을 분기하여 호출합니다.

**`order-saga-consumer/src/main/java/com/example/ordersagaconsumer/application/service/ProcessOrderSagaEventService.java`**
```java
// ... imports ...
@Service
public class ProcessOrderSagaEventService implements ProcessOrderSagaEventUseCase {

    private final LoadOrderSagaPort loadOrderSagaPort; // Output Port
    private final CouponServicePort couponServicePort;   // Output Port
    private final PointServicePort pointServicePort;     // Output Port
    private final UpdateOutboxMessagePort updateOutboxMessagePort; // Output Port
    private final SagaStatusTransitionService sagaStatusTransitionService; // Saga 상태 전이 관리 서비스

    // ... (생성자 생략) ...

    @Override
    public void process(String orderId, String status) {
        if (orderId == null || orderId.isBlank()) {
            System.err.println("### OrderSaga lookup skipped ### : empty orderId");
            return;
        }

        // orderId로 Saga 정보 조회 (SagaInfo는 OutboxMessage에서 파생된 도메인 객체)
        OrderSagaInfo info = loadOrderSagaPort.findByOrderId(orderId)
                .orElse(null);

        if (info == null) {
            System.err.println("### OrderSaga not found ### : orderId=" + orderId + " status=" + status);
            return;
        }

        System.out.println("### OrderSaga details ### : orderId=" + orderId + " status=" + status
                + " couponNumber=" + info.couponNumber() + " pointNumber=" + info.pointNumber());

        OrderSagaStatus sagaStatus = parseSagaStatus(status); // 이벤트로부터 Saga Status 파싱
        if (sagaStatus == null) {
            System.err.println("### OrderSaga status skipped ### : unsupported status=" + status);
            return;
        }

        if (sagaStatus == OrderSagaStatus.Reserved) { // Saga 상태가 Reserved이면 확정 로직 수행
            handleConfirm(orderId, info);
            return;
        }

        if (sagaStatus == OrderSagaStatus.Compensating) { // Saga 상태가 Compensating이면 보상 로직 수행
            handleCompensate(orderId, info);
        }
    }
    // ... (handleConfirm, handleCompensate, parseSagaStatus 헬퍼 메서드는 Chapter 7에서 상세 설명) ...
}
```
**설명:** `process` 메서드는 수신된 `orderId`와 `status`를 기반으로 `OrderSagaInfo`를 조회하고, `sagaStatus` 값에 따라 `handleConfirm` 또는 `handleCompensate` 메서드를 호출합니다. 이 메서드들은 `coupon-service`나 `point-service`에 실제 확정/보상 요청을 보내는 역할을 합니다.

## 5. 실습 체크포인트

`order-saga-consumer`를 실행하고, `order-orchestrator`에서 이벤트를 발행했을 때 컨슈머가 이벤트를 소비하고 처리하는 과정을 확인합니다.

1.  **필수 서비스 실행:**
    *   Chapter 4에서 배포한 Kafka 클러스터가 실행 중인지 확인합니다.
    *   `coupon-service`, `point-service` (Chapter 2에서 다룸, `seminar/05_point_service.md` 참고하여 `point-service`도 실행) 및 `order-orchestrator`가 모두 실행 중인지 확인합니다.
    *   `point-service`는 `point-service` 폴더로 이동 후 `./gradlew bootRun` 명령어로 실행하며, 기본 포트는 `8082`입니다.
2.  **`order-saga-consumer` 실행:**
    *   새로운 터미널을 열고 `order-saga-consumer` 프로젝트 폴더(`order-saga-consumer/`)로 이동한 후 `./gradlew bootRun` 명령어로 `order-saga-consumer`를 실행합니다. (또는 IDE에서 `OrderSagaConsumerApplication.java`를 실행)
    *   `order-saga-consumer`는 기본적으로 `8083` 포트로 실행됩니다. `application.yml`의 Kafka 설정이 올바른지 확인하세요.
3.  **`order-orchestrator`를 통한 주문 생성:**
    *   `order-orchestrator/src/test/httprequest/01_orderOrchestratorTest.http` 파일에서 "주문 생성 요청 (Happy Path 예시)"를 사용하여 API를 호출합니다.
    *   **예상 결과:** `order-orchestrator` 로그에 이벤트 발행 메시지가, `order-saga-consumer` 로그에는 이벤트 소비 메시지(`### Kafka Event Consumed ###`, `### OrderSaga details ###`)가 출력되는 것을 확인할 수 있습니다.
4.  **H2 Console을 통해 DB 확인 (선택 사항):**
    *   `order-orchestrator`의 H2 DB에서 `SELECT * FROM OUTBOX_MESSAGE;`를 실행하여, `couponStatus`와 `pointStatus`가 `Reserved`로 유지되는 것을 확인합니다. (`order-saga-consumer`는 아직 이 상태를 `Completed`로 업데이트하지 않습니다. 이 로직은 Chapter 7에서 다룹니다.)

---
이제 `order-saga-consumer`가 Kafka 이벤트를 성공적으로 소비하고 Saga의 다음 단계를 처리할 준비를 마쳤습니다. 다음 챕터에서는 `order-saga-consumer`가 수신된 이벤트에 따라 `confirm` 또는 `compensate` 로직을 실제 참여자 서비스에 요청하여 Saga의 핵심인 **보상 트랜잭션**을 구현하는 방법을 상세히 알아봅니다.
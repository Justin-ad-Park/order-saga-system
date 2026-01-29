# Chapter 3: Outbox Pattern으로 신뢰성 확보

## 1. 개요: 분산 트랜잭션의 난제와 Outbox Pattern의 등장

이전 챕터에서 `order-orchestrator`와 `coupon-service` 간의 동기(HTTP) 통신을 살펴보았습니다. 하지만 분산 시스템에서는 서비스 간의 통신이 성공하더라도, `order-orchestrator`가 자신의 데이터베이스에 변경사항을 커밋하는 과정에서 실패할 수 있습니다. 이 경우 주문은 성공했지만, 쿠폰은 예약되지 않거나, 그 반대의 상황이 발생하여 시스템 간 **데이터 불일치(Data Inconsistency)**가 발생합니다. 이는 분산 트랜잭션 환경에서 발생하는 고질적인 문제입니다.

이러한 문제를 해결하기 위한 강력한 패턴 중 하나가 바로 **Outbox Pattern**입니다. 본 챕터에서는 Outbox Pattern의 원리와 도입 배경, 그리고 이 패턴이 Saga 상태 추적 및 신뢰성 있는 이벤트 발행에 어떻게 활용되는지 깊이 있게 학습합니다.

### 핵심 학습 목표
*   분산 시스템에서 데이터 일관성 문제가 발생하는 이유를 이해합니다.
*   Outbox Pattern이 로컬 트랜잭션과 이벤트 발행의 원자성을 어떻게 보장하는지 학습합니다.
*   `outbox_message` 테이블이 Saga의 현재 상태를 추적하는 신뢰성 있는 기록으로 활용되는 방식을 이해합니다.

## 2. Outbox Pattern 상세 이해

**문제:** 일반적인 이벤트 기반 아키텍처에서 서비스는 비즈니스 로직을 처리한 후 데이터베이스에 변경사항을 커밋하고, 이어서 이벤트 메시지를 발행합니다. 이때 다음 두 가지 문제가 발생할 수 있습니다.
1.  **DB 커밋은 성공했으나 이벤트 발행에 실패하는 경우:** 이 경우 다른 서비스들은 이벤트 소식을 받지 못해 시스템 간의 상태가 불일치하게 됩니다.
2.  **이벤트 발행은 성공했으나 DB 커밋에 실패하는 경우:** 이 경우 이벤트는 발행되었으나 해당 서비스의 상태는 변경되지 않아 역시 불일치가 발생합니다.

**해결책: Outbox Pattern**
Outbox Pattern은 **비즈니스 로직의 변경사항과 발행할 이벤트 메시지를 같은 로컬 트랜잭션 안에서 데이터베이스에 함께 저장**함으로써 이 문제를 해결합니다.

1.  비즈니스 로직 처리와 함께, 발행할 이벤트 메시지를 `outbox_message` 테이블에 저장합니다.
2.  이 두 작업은 하나의 DB 트랜잭션으로 묶여 원자적으로 처리됩니다. 즉, 둘 다 성공하거나 둘 다 실패합니다.
3.  `outbox_message` 테이블에 저장된 메시지는 별도의 **메시지 중계기(Message Relayer)** 프로세스(예: Polling Publisher, Debezium 등)에 의해 읽혀져 메시지 브로커(예: Kafka)로 발행됩니다.

**우리 프로젝트에서의 Outbox Pattern 활용:**
현재 프로젝트에서는 `order-orchestrator`가 주문을 생성하고 `coupon-service`, `point-service`와 같은 외부 서비스를 호출하기 전, Saga의 초기 상태를 `order_saga` 테이블과 함께 `outbox_message` 테이블에 기록합니다. `outbox_message` 테이블은 Saga의 현재 진행 상태(예: 쿠폰 예약 상태, 포인트 예약 상태)를 저장하는 **신뢰성 있는 기록 저장소** 역할을 합니다.

**참고:** `OUTBOX_MESSAGE`를 통한 '메시지 중계' 로직은 아직 구현되지 않았습니다. 현재는 `order-orchestrator`가 외부 서비스 호출 완료 후 Kafka 이벤트를 **직접 발행**하는 방식을 사용합니다. Outbox Pattern은 현재 Saga 상태를 DB 트랜잭션과 함께 안정적으로 기록하는 용도로 사용됩니다. (Chapter 5에서 자세히 설명)

## 3. Outbox Pattern 관련 Git 이력

`order-orchestrator`에서 `OutboxMessage` 테이블의 초기 구현 및 Saga 상태를 기록하는 로직과 관련된 주요 Git 커밋입니다.

| 커밋 ID | 날짜 | 주요 변경 요약 |
|---|---|---|
| `d95cb17` | 2025-12-30 | `outbox_message` 테이블에 MSA 상태 저장 로직 추가 |
| `0d2221b` | 2025-12-30 | `outbox_message`에 `pointStatus` 컬럼 추가 |
| `982ec0a` | 2025-12-31 | `saga_status`가 결과에 맞게 `Reserved` 또는 `Compensating`으로 업데이트되도록 로직 수정 |

**(실습 가이드: Git 커밋 확인)**
1.  `git checkout d95cb17` 명령어로 해당 커밋 시점으로 이동하여 `OutboxMessage`의 초기 구조와 `MSAStatus`가 추가된 변경사항을 확인해 보세요.
2.  `git diff 0d2221b~1 0d2221b` 명령어로 `pointStatus`가 추가된 `diff`를 확인할 수 있습니다.

## 4. 핵심 코드 스니펫: Outbox 메시지 저장

### 4.1. `OutboxMessage` 도메인 객체 정의

`outbox_message` 테이블의 스키마와 매핑되는 `OutboxMessage` 도메인 객체입니다. Saga의 `orderId`, `payload`, 그리고 각 MSA(Coupon, Point, Order, Payment)의 상태 및 전체 Saga의 `sagaStatus`를 포함합니다.

**`order-orchestrator/src/main/java/com/example/orderorchestrator/domain/outbox/OutboxMessage.java`**
```java
// ... imports ...
public class OutboxMessage {

    private final String orderId;       // 주문 ID (식별자)
    private final String payload;       // 메시지 payload (현재는 "{}"로 초기화)

    // 각 MSA의 현재 처리 상태
    private MSAStatus couponStatus;
    private MSAStatus pointStatus;
    private MSAStatus orderStatus;
    private MSAStatus paymentStatus; // 현재 프로젝트에서는 사용되지 않음

    private OrderSagaStatus sagaStatus; // 전체 Saga의 상태 (InProgress, Reserved, Compensating 등)

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ... (생성자, getter, 상태 업데이트 메서드 생략) ...

    // OutboxMessage 초기 생성을 위한 팩토리 메서드
    public static OutboxMessage initial(
            String orderId,
            String payload,
            MSAStatus couponStatus, // 초기 쿠폰 상태
            MSAStatus pointStatus   // 초기 포인트 상태
    ) {
        return new OutboxMessage(
                orderId,
                payload,
                couponStatus,
                pointStatus,
                MSAStatus.InProgress,  // 주문 MSA 요청 시작 (항상 InProgress)
                OrderSagaStatus.InProgress, // Saga 초기 상태
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
```

### 4.2. `CreateOrderService`에서 `OutboxMessage` 저장

`CreateOrderService`는 주문 생성과 함께 `order_saga`와 `outbox_message`를 하나의 트랜잭션 안에서 저장합니다. `OutboxMessage.initial()` 팩토리 메서드를 사용하여 초기 상태의 `OutboxMessage`를 생성합니다.

**`order-orchestrator/src/main/java/com/example/orderorchestrator/application/service/CreateOrderService.java`**
```java
// ... imports ...
@Service
@Transactional
public class CreateOrderService implements CreateOrderUseCase {

    private final SaveOrderSagaPort saveOrderSagaPort;
    private final SaveOutboxMessagePort saveOutboxMessagePort; // Output Port

    // ... (생성자 생략) ...

    @Override
    public CreateOrderResult createOrder(CreateOrderCommand command) {
        // ... (orderId, sagaId, orderItems, OrderSaga 생성 및 저장 로직 생략) ...

        // 각 MSA의 초기 상태를 결정 (예: 쿠폰 번호가 없으면 NotUsed)
        MSAStatus couponStatus = resolveUsageStatus(command.couponNumber());
        MSAStatus pointStatus = resolveUsageStatus(command.pointNumber());

        // Outbox 메시지 생성 (초기 payload는 비어있으며, 각 MSA 상태와 Saga 상태를 기록)
        OutboxMessage message = OutboxMessage.initial(
                savedSaga.orderId(),
                "{}", // payload (TODO: 실제 JSON으로 교체)
                couponStatus,
                pointStatus
        );

        saveOutboxMessagePort.save(message); // Outbox 메시지 저장 (Output Port를 통해)

        // ... (결과 반환 로직 생략) ...
    }
    // ... (헬퍼 메서드 생략)
}
```

### 4.3. `OutboxMessagePersistenceAdapter`를 통한 DB 저장

`SaveOutboxMessagePort` (Output Port)는 `OutboxMessagePersistenceAdapter`에 의해 구현됩니다. 이 어댑터는 JPA Repository를 사용하여 `OutboxMessage` 객체를 `outbox_message` 테이블에 저장합니다.

**`order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/OutboxMessagePersistenceAdapter.java`**
```java
// ... imports ...
@Repository
@Transactional // 단일 트랜잭션 보장
public class OutboxMessagePersistenceAdapter implements SaveOutboxMessagePort, UpdateOutboxMessagePort {

    private final OutboxMessageJpaRepository outboxMessageJpaRepository;

    public OutboxMessagePersistenceAdapter(OutboxMessageJpaRepository outboxMessageJpaRepository) {
        this.outboxMessageJpaRepository = outboxMessageJpaRepository;
    }

    @Override
    public OutboxMessage save(OutboxMessage message) {
        OutboxMessageJpaEntity entity = new OutboxMessageJpaEntity(
                message.orderId(),
                message.payload(),
                message.couponStatus(),
                message.pointStatus(),
                message.orderStatus(),
                message.sagaStatus(),
                message.createdAt(),
                message.updatedAt()
        );

        OutboxMessageJpaEntity saved = outboxMessageJpaRepository.save(entity); // JPA를 통해 DB에 저장

        // 저장된 엔티티로부터 도메인 객체로 다시 변환하여 반환
        return new OutboxMessage(
                saved.getOrderId(),
                saved.getPayload(),
                saved.getCouponStatus(),
                saved.getPointStatus(),
                saved.getOrderStatus(),
                saved.getSagaStatus(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    // ... (updateCouponStatus, updatePointStatus, updateSagaStatus 메서드 생략) ...
}
```

## 5. 실습 체크포인트

`order-orchestrator`를 실행하고 주문을 생성한 후 `outbox_message` 테이블에 어떤 데이터가 저장되는지 확인합니다.

1.  **`order-orchestrator` 실행:** (Chapter 2의 실습 가이드 참조)
    *   `coupon-service`와 `order-orchestrator`가 모두 실행 중인지 확인합니다.
2.  **주문 생성 API 호출:**
    *   `order-orchestrator/src/test/httprequest/01_orderOrchestratorTest.http` 파일의 "주문 생성 요청 (Happy Path 예시)"를 사용하여 API를 호출합니다.
3.  **H2 Console을 통해 DB 확인:**
    *   브라우저에서 `http://localhost:8080/h2-console`에 접속하여 `order-orchestrator`의 H2 DB에 접근합니다.
    *   **`SELECT * FROM OUTBOX_MESSAGE;`** 쿼리를 실행하여 새로운 레코드가 추가되었는지 확인합니다.
    *   **확인 사항:** `orderId`, `sagaStatus`가 `InProgress`로, `couponStatus`와 `pointStatus`가 `Reserved` (만약 요청에 쿠폰/포인트가 포함되었다면)로 기록되었는지 확인합니다. `payload`는 현재 "{}"로 저장되어 있을 것입니다.

---
`Outbox Pattern`을 통해 우리는 분산 트랜잭션의 첫 번째 과제인 데이터 일관성을 확보할 기반을 마련했습니다. 이제 다음 챕터에서는 서비스 간 비동기 통신을 위한 핵심 인프라인 **Apache Kafka**를 어떻게 구축하고 활용하는지 알아봅니다.
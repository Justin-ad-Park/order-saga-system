# Chapter 3: Outbox Pattern으로 신뢰성 보장하기

`order-orchestrator`와 `coupon-service`를 연동했지만, 분산 시스템의 큰 난관인 '데이터 정합성' 문제에 부딪혔습니다. 예를 들어, `ORDERS` 테이블에 주문 정보는 저장(Commit)되었지만, 네트워크 오류나 `coupon-service`의 일시적인 장애로 쿠폰 사용 요청이 실패하거나 유실될 수 있습니다. 이를 **이중 쓰기(Dual-writes)** 문제라고 합니다.

본 챕터에서는 이 문제를 해결하고 신뢰성 있는 메시지 전달을 위한 **기반**을 마련하는 **Outbox Pattern(또는 Transactional Outbox)** 을 어떻게 도입했는지 알아봅니다.

## 1. Outbox Pattern이란?

Outbox Pattern은 서비스의 데이터베이스 트랜잭션 안에 '발행할 메시지 기록'을 포함시키는 기법입니다.

1.  **하나의 트랜잭션으로 묶기:** 서비스는 비즈니스 데이터를 저장하는 작업과, 발행할 메시지를 **같은 데이터베이스의 `OUTBOX` 테이블**에 저장하는 작업을 **하나의 원자적 트랜잭션(Single Atomic Transaction)** 으로 묶어서 처리합니다.
    *   _이 프로젝트에서는 `OUTBOX_MESSAGE` 테이블에 Saga의 진행 상태 및 참여 서비스들의 상태를 기록하여, 트랜잭션의 원자성과 Saga 상태 추적을 위한 **신뢰성 있는 기록(reliable record)**으로 활용합니다._
2.  **메시지 중계 (Full Outbox Pattern의 구성요소):** 별도의 **메시지 릴레이(Message Relay)** 프로세스가 `OUTBOX` 테이블을 주기적으로 폴링(polling)합니다.
3.  **메시지 발행 및 상태 업데이트 (Full Outbox Pattern의 구성요소):** 릴레이는 `OUTBOX` 테이블에서 '발행되지 않은' 메시지를 가져와 메시지 브로커(예: Kafka)로 안정적으로 발행합니다. 발행에 성공하면 해당 메시지를 '발행 완료' 상태로 업데이트하여 중복 발행을 방지합니다.
    *   _이 프로젝트에서는 2번과 3번의 '메시지 릴레이'를 통한 이벤트 발행 로직은 아직 구현되지 않았습니다. 현재 이벤트 발행은 컨트롤러에서 직접 수행하며, `OUTBOX_MESSAGE`는 Saga 상태 기록 용도로 사용됩니다._

이 패턴을 사용하면, 서비스의 DB 트랜잭션이 성공했다면 발행할 메시지 또한 DB에 안정적으로 기록됨을 보장할 수 있습니다.

## 2. 주요 Git 이력

아래 커밋들은 `point-service`를 추가하는 과정과 함께 Outbox Pattern 및 Saga 상태 관리를 도입하는 과정을 보여줍니다.

```
* 982ec0a | 2025-12-31 | saga_status가 결과에 맞게 Reserved 또는 Compensating으로 업데이트 되도록 로직 수정
* d95cb17 | 2025-12-30 | outbox_message MSA 상태 저장 로직 추가
* 193e5e2 | 2025-12-29 | First commit for Point MSA
```

## 3. 핵심 코드 스니펫

### 1) `OutboxMessage` 도메인 객체 정의

먼저 발행할 메시지의 내용을 담는 `OutboxMessage` 도메인 객체를 정의합니다. 이 객체는 Saga의 진행 상태(쿠폰, 포인트 처리 상태 등)를 추적하는 역할도 겸합니다.

**`order-orchestrator/.../domain/outbox/OutboxMessage.java`**
```java
public class OutboxMessage {

    private final String orderId;
    private final String payload; // 메시지 payload(JSON)

    private MSAStatus couponStatus;
    private MSAStatus pointStatus;
    private MSAStatus orderStatus;

    private OrderSagaStatus sagaStatus;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ... (생성자 및 getter/setter 생략)
}
```

### 2) `OutboxMessage` 팩토리 메서드를 이용한 생성

`OutboxMessage`는 초기 상태를 설정하여 생성하는 `initial` 팩토리 메서드를 제공합니다. 이때 각 참여 서비스의 초기 상태와 Saga의 전체 상태를 `InProgress`로 설정합니다.

**`order-orchestrator/.../domain/outbox/OutboxMessage.java`**
```java
public class OutboxMessage {
    // ... (필드 및 생성자 생략)

    // Outbox 최초 생성 시 사용하는 팩토리
    public static OutboxMessage initial(
            String orderId,
            String payload,
            MSAStatus couponStatus,
            MSAStatus pointStatus
    ) {
        return new OutboxMessage(
                orderId,
                payload,
                couponStatus,
                pointStatus,
                MSAStatus.InProgress,  // ✅ 주문 MSA 요청 시작
                OrderSagaStatus.InProgress, // ✅ Saga 전체 상태 InProgress
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
    // ...
}
```

### 3) `CreateOrderService`에서 `OutboxMessage` 트랜잭션 저장

`CreateOrderService`의 `createOrder` 메서드에 `@Transactional` 어노테이션을 적용하여, 비즈니스 데이터(`OrderSaga`) 저장과 `OutboxMessage` 저장이 하나의 트랜잭션으로 묶이도록 보장합니다.

**`order-orchestrator/.../application/service/CreateOrderService.java`**
```java
@Service
@Transactional // ✅ 이 어노테이션이 핵심입니다.
public class CreateOrderService implements CreateOrderUseCase {

    private final SaveOrderSagaPort saveOrderSagaPort;
    private final SaveOutboxMessagePort saveOutboxMessagePort;

    // ... constructor ...

    @Override
    public CreateOrderResult createOrder(CreateOrderCommand command) {
        // ...
        // 3) OrderSaga 엔티티 생성 (초기 상태: InProgress)
        OrderSaga saga = OrderSaga.create(/* ... */);

        // 4) Saga 저장 (Output Port 사용)
        OrderSaga savedSaga = saveOrderSagaPort.save(saga);

        // 5) Outbox 메시지 생성 (Saga 상태 추적 용도)
        MSAStatus couponStatus = resolveUsageStatus(command.couponNumber());
        MSAStatus pointStatus = resolveUsageStatus(command.pointNumber());
        OutboxMessage message = OutboxMessage.initial(
                savedSaga.orderId(), "{}", couponStatus, pointStatus
        );

        // 6) Outbox 저장 (Output Port 사용)
        // 👇 saga 저장과 message 저장은 같은 트랜잭션으로 묶여 실행됩니다.
        // 만약 이 과정에서 예외가 발생하면 둘 다 롤백됩니다.
        saveOutboxMessagePort.save(message);

        return CreateOrderResult.of(/* ... */);
    }
    // ...
}
```
`@Transactional` 덕분에 `saveOrderSagaPort.save()`와 `saveOutboxMessagePort.save()`는 둘 다 성공하거나, 둘 다 실패하게 됩니다. 이로써 '주문은 성공했는데 메시지 저장은 실패하는' 데이터 불일치 상황을 원천적으로 방지할 수 있습니다. `OUTBOX_MESSAGE` 테이블은 Saga의 진행 상황을 안정적으로 기록하는 역할을 수행합니다.

---
이제 우리는 Outbox Pattern을 통해 서비스 간의 데이터 정합성을 보장할 수 있는 기반을 마련했으며, Saga의 상태를 신뢰성 있게 기록하고 있습니다. 다음 챕터에서는 비동기 메시징 시스템의 핵심인 **Kafka**를 구축하고, 현재 이벤트가 어떻게 발행되는지 알아보겠습니다.
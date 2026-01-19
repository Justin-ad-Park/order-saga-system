# 06. Outbox와 Saga 상태 전이

## 목표
- outbox 메시지와 saga_status 상태 전이 규칙을 이해한다.

## 스토리라인
- 이벤트 전파가 안정적이지 않아 outbox를 도입.
- saga_status 전이가 의미 있게 정의되어야 테스트가 안정됨.

## 관련 커밋
- `d95cb17`, `0d2221b`, `982ec0a`, `0531530`

## 커밋 변경 요약
| 커밋 | 주요 변경 요약 | 체크아웃 |
| --- | --- | --- |
| `d95cb17` | outbox_message MSA 상태 저장 로직 추가 | `git checkout d95cb17` |
| `0d2221b` | outboxMessage에 pointStatus 컬럼 추가 | `git checkout 0d2221b` |
| `982ec0a` | saga_status가 결과에 맞게 Reserved 또는 Compensating으로 업데이트 되도록 로직 수정 | `git checkout 982ec0a` |
| `0531530` | updateSagaStatus 메서드 리팩터링 | `git checkout 0531530` |

## 핵심 개념
- outbox_message와 order_saga 테이블 역할
- 상태 전이(Reserved/Compensating/Completed)

## 기술/기능/프로세스
- 기술: JPA/JDBC, outbox 테이블
- 기능: saga_status 전이, outbox_message 업데이트
- MSA: 오케스트레이터가 상태 추적
- EDA: outbox 기반 이벤트 발행 흐름 확립
## 데모/실습
- 테이블/엔티티 확인: `order-orchestrator/src/main/java/.../OutboxMessage.java`
- 상태 업데이트 로직 확인: `order-orchestrator/.../OutboxMessageStatusJdbcAdapter.java`

## 커밋 상세
### d95cb17 outbox_message MSA 상태 저장 로직 추가
- 주요 변경: outbox_message MSA 상태 저장 로직 추가
- 핵심 코드: `order-orchestrator/src/main/java/com/example/orderorchestrator/domain/outbox/OutboxMessage.java`
```java
public class OutboxMessage {
//--- 생략 ...
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
                MSAStatus.InProgress,  // 주문 MSA 요청 시작
                MSAStatus.InProgress,  // 결제 MSA 요청 시작
                OrderSagaStatus.InProgress,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
//--- 생략 ...
}
```
- 설명: Outbox에 이벤트를 적재해 DB 트랜잭션과 이벤트 발행을 분리한다.

### 0d2221b outboxMessage에 pointStatus 컬럼 추가
- 주요 변경: outboxMessage에 pointStatus 컬럼 추가
- 핵심 코드: `order-orchestrator/src/main/java/com/example/orderorchestrator/domain/outbox/OutboxMessage.java`
```java
public class OutboxMessage {
//--- 생략 ...
    private final String payload;               // 메시지 payload(JSON)

    private MSAStatus couponStatus;
    private MSAStatus pointStatus;
    private MSAStatus orderStatus;
    private MSAStatus paymentStatus;

    private OrderSagaStatus sagaStatus;

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OutboxMessage(
            String orderId,
            String payload,
            MSAStatus couponStatus,
            MSAStatus pointStatus,
            MSAStatus orderStatus,
            MSAStatus paymentStatus,
            OrderSagaStatus sagaStatus,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.orderId = orderId;
        this.payload = payload;
        this.couponStatus = couponStatus;
        this.pointStatus = pointStatus;
        this.orderStatus = orderStatus;
        this.paymentStatus = paymentStatus;
        this.sagaStatus = sagaStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
//--- 생략 ...
}
```
- 설명: Outbox에 이벤트를 적재해 DB 트랜잭션과 이벤트 발행을 분리한다.

### 982ec0a saga_status가 결과에 맞게 Reserved 또는 Compensating으로 업데이트 되도록 로직 수정
- 주요 변경: saga_status가 결과에 맞게 Reserved 또는 Compensating으로 업데이트 되도록 로직 수정
- 핵심 코드: `order-orchestrator/src/main/java/com/example/orderorchestrator/application/service/UpdateOrderSagaStatusService.java`
```java
public class UpdateOrderSagaStatusService implements UpdateOrderSagaStatusUseCase {
//--- 생략 ...
    public UpdateOrderSagaStatusService(UpdateOrderSagaStatusPort updateOrderSagaStatusPort) {
        this.updateOrderSagaStatusPort = updateOrderSagaStatusPort;
    }
//--- 생략 ...
}
```
- 설명: Saga 상태 전이를 명시해 흐름의 단계가 코드로 드러나게 한다.

### 0531530 updateSagaStatus 메서드 리팩터링
- 주요 변경: updateSagaStatus 메서드 리팩터링
- 핵심 코드: `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/application/port/out/UpdateOutboxMessagePort.java`
```java
public interface UpdateOutboxMessagePort {
//--- 생략 ...
}
```
- 설명: Outbox에 이벤트를 적재해 DB 트랜잭션과 이벤트 발행을 분리한다.

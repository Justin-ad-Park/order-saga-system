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


# 사가 상태 + Outbox 저장

## 목표
사가 상태와 Outbox 기록이 왜 필요한지 이해한다.


## Outbox 모델
`order-orchestrator/src/main/java/com/example/orderorchestrator/domain/outbox/OutboxMessage.java`
```java
package com.example.orderorchestrator.domain.outbox;

import java.time.Instant;

import com.example.common.status.MSAStatus;
import com.example.common.status.OrderSagaStatus;

import java.time.LocalDateTime;

public class OutboxMessage {

    private final String orderId;               // 주문 ID
    private final String payload;               // 메시지 payload(JSON)

    private MSAStatus couponStatus;
    private MSAStatus pointStatus;
    private MSAStatus orderStatus;

    private OrderSagaStatus sagaStatus;

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OutboxMessage(
            String orderId,
            String payload,
            MSAStatus couponStatus,
            MSAStatus pointStatus,
            MSAStatus orderStatus,
            OrderSagaStatus sagaStatus,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.orderId = orderId;
        this.payload = payload;
        this.couponStatus = couponStatus;
        this.pointStatus = pointStatus;
        this.orderStatus = orderStatus;
        this.sagaStatus = sagaStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

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
                MSAStatus.InProgress,  // 주문 MSA 요청 시작
                OrderSagaStatus.InProgress,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // getter
    public String orderId() { return orderId; }
    public String payload() { return payload; }

    public MSAStatus couponStatus() { return couponStatus; }
    public MSAStatus pointStatus() { return pointStatus; }
    public MSAStatus orderStatus() { return orderStatus; }

    public OrderSagaStatus sagaStatus() { return sagaStatus; }

    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime updatedAt() { return updatedAt; }

    // 상태 변경 로직
    public void updateSagaStatus(OrderSagaStatus newStatus) {
        this.sagaStatus = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public void markCouponStatus(MSAStatus status) {
        this.couponStatus = status;
        this.updatedAt = LocalDateTime.now();
    }

    public void markPointStatus(MSAStatus status) {
        this.pointStatus = status;
        this.updatedAt = LocalDateTime.now();
    }

    public void markOrderStatus(MSAStatus status) {
        this.orderStatus = status;
        this.updatedAt = LocalDateTime.now();
    }
}

```

## Outbox 저장/업데이트
`order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/OutboxMessagePersistenceAdapter.java`
```java
// src/main/java/com/example/orderorchestrator/adapter/out/persistence/OutboxMessagePersistenceAdapter.java
package com.example.orderorchestrator.adapter.out.persistence;

import com.example.orderorchestrator.application.port.out.SaveOutboxMessagePort;
import com.example.orderorchestrator.application.port.out.UpdateOutboxMessagePort;
import com.example.orderorchestrator.domain.outbox.OutboxMessage;
import com.example.common.status.MSAStatus;
import com.example.common.status.OrderSagaStatus;
import com.example.orderorchestrator.adapter.out.persistence.jpa.OutboxMessageJpaRepository;
import com.example.orderorchestrator.adapter.out.persistence.jpa.entity.OutboxMessageJpaEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
@Transactional
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

        OutboxMessageJpaEntity saved = outboxMessageJpaRepository.save(entity);

        // id는 현재 도메인 OutboxMessage에 없으니,
        // 필요하면 나중에 OutboxMessage에 id 필드를 추가하고 여기서 반영해도 됨.
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

    @Override
    public void updateCouponStatus(String orderId, MSAStatus status) {
        int updated = outboxMessageJpaRepository.updateCouponStatus(orderId, status, LocalDateTime.now());
        if (updated == 0) {
            throw new IllegalArgumentException("Outbox message not found: " + orderId);
        }
    }

    @Override
    public void updatePointStatus(String orderId, MSAStatus status) {
        int updated = outboxMessageJpaRepository.updatePointStatus(orderId, status, LocalDateTime.now());
        if (updated == 0) {
            throw new IllegalArgumentException("Outbox message not found: " + orderId);
        }
    }

    @Override
    public void updateSagaStatus(String orderId, OrderSagaStatus status) {
        int updated = outboxMessageJpaRepository.updateSagaStatus(orderId, status, LocalDateTime.now());
        if (updated == 0) {
            throw new IllegalArgumentException("Outbox message not found: " + orderId);
        }
    }
}

```


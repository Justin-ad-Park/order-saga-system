# Chapter 7: Saga 보상 트랜잭션 (Compensating Transaction) 구현

Saga 패턴의 핵심은 분산된 서비스들 간의 트랜잭션을 '결국에는(eventually)' 일관성 있게 만드는 것입니다. 모든 과정이 성공하면 문제가 없지만, 중간에 하나라도 실패했을 때 시스템을 원래 상태에 가깝게 되돌리는 **보상 트랜잭션(Compensating Transaction)** 메커니즘이 반드시 필요합니다.

본 챕터에서는 `order-saga-consumer`가 어떻게 실패 상황을 감지하고, 각 서비스에 보상 조치를 요청하여 데이터 정합성을 맞추는지 알아봅니다.

## 1. 보상 트랜잭션 시나리오

다음과 같은 실패 시나리오를 가정해 봅시다.

1.  `order-saga-consumer`가 '주문 시작' 이벤트를 받습니다.
2.  `coupon-service`에 쿠폰 사용을 요청했고, 성공적으로 처리되었습니다. (쿠폰이 '사용됨' 상태로 변경)
3.  `point-service`에 포인트 사용을 요청했지만, 네트워크 오류나 서버 장애로 **실패**했습니다.

이 상태로 끝나면, 고객의 포인트는 차감되지 않았지만 쿠폰만 사용된 채로 주문이 불완전하게 남게 됩니다. 이를 해결하기 위해 Saga는 이미 성공한 앞선 단계를 거꾸로 거슬러 올라가며 취소 조치를 수행해야 합니다. 즉, `coupon-service`에 "방금 사용했던 쿠폰을 다시 원상복구하라"고 요청해야 합니다. 이것이 바로 보상 트랜잭션입니다.

## 2. 주요 Git 이력

아래 커밋들은 보상 트랜잭션 로직과 멱등성 처리에 관련된 구현 과정을 보여줍니다.

```
* 7d9e662 | 2026-01-06 | 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
* 0b73be2 | 2026-01-06 | ### Saga 컨슈머 confirm, compensate 로직 추가 ###
* 66c93ca | 2026-01-06 | confirm, compansate API를 point-service에도 동일한 방식으로 추가
* 091c2a7 | 2026-01-05 | coupon-service에 보상(compensateCoupon) API 추가
* 982ec0a | 2025-12-31 | saga_status가 결과에 맞게 Reserved 또는 Compensating으로 업데이트 되도록 로직 수정
```

## 3. 핵심 코드 스니펫

### 1) 이벤트 수신 (`OrderSagaEventConsumer`)

`order-saga-consumer`는 Kafka 이벤트를 수신하는 Input Adapter인 `OrderSagaEventConsumer`로부터 시작하여 `ProcessOrderSagaEventUseCase`를 통해 비즈니스 로직을 처리합니다.

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

    @KafkaListener(
            topics = "${order.saga.events.topic}",
            groupId = "${order.saga.events.consumer-group:order-saga-consumer}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        OrderSagaEventPayload payload = readPayload(record.value());
        if (payload == null) {
            return;
        }
        // ✅ 수신된 이벤트의 상태에 따라 Saga 처리 로직 위임
        processOrderSagaEventUseCase.process(payload.orderId(), payload.status());
    }

    private OrderSagaEventPayload readPayload(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, OrderSagaEventPayload.class);
        } catch (Exception ex) {
            System.out.println("### Kafka payload parse failed ### : message=" + ex.getMessage()
                    + " payload=" + rawPayload);
            return null;
        }
    }
}
```

### 2) `ProcessOrderSagaEventService`의 Saga 흐름 제어 및 보상 로직

`ProcessOrderSagaEventService`는 `ProcessOrderSagaEventUseCase` 인터페이스의 구현체로, 수신된 이벤트의 Saga 상태(Reserved 또는 Compensating)에 따라 적절한 `handleConfirm` 또는 `handleCompensate` 메서드를 호출하여 Saga의 흐름을 제어합니다.

**`order-saga-consumer/.../application/service/ProcessOrderSagaEventService.java`**
```java
package com.example.ordersagaconsumer.application.service;

import com.example.ordersagaconsumer.application.port.in.ProcessOrderSagaEventUseCase;
import com.example.ordersagaconsumer.application.port.out.CouponServicePort;
import com.example.ordersagaconsumer.application.port.out.LoadOrderSagaPort;
import com.example.ordersagaconsumer.application.port.out.PointServicePort;
import com.example.ordersagaconsumer.application.port.out.UpdateOutboxMessagePort;
import com.example.ordersagaconsumer.domain.model.OrderSagaInfo;
import com.example.common.status.MSAStatus;
import com.example.common.status.OrderSagaStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProcessOrderSagaEventService implements ProcessOrderSagaEventUseCase {

    private final LoadOrderSagaPort loadOrderSagaPort;
    private final CouponServicePort couponServicePort;
    private final PointServicePort pointServicePort;
    private final UpdateOutboxMessagePort updateOutboxMessagePort;
    private final SagaStatusTransitionService sagaStatusTransitionService;

    public ProcessOrderSagaEventService(
            LoadOrderSagaPort loadOrderSagaPort,
            CouponServicePort couponServicePort,
            PointServicePort pointServicePort,
            UpdateOutboxMessagePort updateOutboxMessagePort,
            SagaStatusTransitionService sagaStatusTransitionService
    ) {
        this.loadOrderSagaPort = loadOrderSagaPort;
        this.couponServicePort = couponServicePort;
        this.pointServicePort = pointServicePort;
        this.updateOutboxMessagePort = updateOutboxMessagePort;
        this.sagaStatusTransitionService = sagaStatusTransitionService;
    }

    @Override
    public void process(String orderId, String status) {
        if (orderId == null || orderId.isBlank()) {
            System.out.println("### OrderSaga lookup skipped ### : empty orderId");
            return;
        }

        OrderSagaInfo info = loadOrderSagaPort.findByOrderId(orderId)
                .orElse(null);

        if (info == null) {
            System.out.println("### OrderSaga not found ### : orderId=" + orderId
                    + " status=" + status);
            return;
        }

        System.out.println("### OrderSaga details ### : orderId=" + orderId
                + " status=" + status
                + " couponNumber=" + info.couponNumber()
                + " pointNumber=" + info.pointNumber());

        OrderSagaStatus sagaStatus = parseSagaStatus(status);
        if (sagaStatus == null) {
            System.out.println("### OrderSaga status skipped ### : unsupported status=" + status);
            return;
        }

        if (sagaStatus == OrderSagaStatus.Reserved) {
            handleConfirm(orderId, info);
            return;
        }

        if (sagaStatus == OrderSagaStatus.Compensating) {
            handleCompensate(orderId, info);
        }
    }

    private void handleConfirm(String orderId, OrderSagaInfo info) {
        boolean couponNeeded = StringUtils.hasText(info.couponNumber());
        boolean pointNeeded = StringUtils.hasText(info.pointNumber());

        boolean couponOk = true;
        boolean pointOk = true;

        if (couponNeeded) {
            couponOk = couponServicePort.confirm(info.couponNumber(), orderId);
            updateOutboxMessagePort.updateCouponStatus(
                    orderId,
                    couponOk ? MSAStatus.Completed : MSAStatus.Failed
            );
        }

        if (pointNeeded) {
            pointOk = pointServicePort.confirm(info.pointNumber(), orderId);
            updateOutboxMessagePort.updatePointStatus(
                    orderId,
                    pointOk ? MSAStatus.Completed : MSAStatus.Failed
            );
        }

        if (couponOk && pointOk) {
            sagaStatusTransitionService.markCompleted(orderId);
        }
    }

    private void handleCompensate(String orderId, OrderSagaInfo info) {
        boolean couponNeeded = StringUtils.hasText(info.couponNumber());
        boolean pointNeeded = StringUtils.hasText(info.pointNumber());

        boolean couponOk = true;
        boolean pointOk = true;

        if (couponNeeded) {
            couponOk = couponServicePort.compensate(info.couponNumber(), orderId);
            updateOutboxMessagePort.updateCouponStatus(
                    orderId,
                    couponOk ? MSAStatus.Compensated : MSAStatus.Failed
            );
        }

        if (pointNeeded) {
            pointOk = pointServicePort.compensate(info.pointNumber(), orderId);
            updateOutboxMessagePort.updatePointStatus(
                    orderId,
                    pointOk ? MSAStatus.Compensated : MSAStatus.Failed
            );
        }

        if (couponOk && pointOk) {
            sagaStatusTransitionService.markCompensated(orderId);
        }
    }

    private OrderSagaStatus parseSagaStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return OrderSagaStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
```

### 3) `Coupon Service`의 보상 API 구현과 멱등성

`coupon-service`의 `ReserveCouponService`에 구현된 `compensateCoupon` 메서드는 쿠폰의 상태를 'AVAILABLE'로 되돌립니다. 특히 `7d9e662` 커밋에서 강조되었듯이, 이 메서드는 멱등성을 보장하도록 설계되어 있습니다.

**`coupon-service/.../application/service/ReserveCouponService.java`**
```java
@Service
@RequiredArgsConstructor
@Transactional
public class ReserveCouponService implements ReserveCouponUseCase, ConfirmCouponUseCase, CompensateCouponUseCase {
    // ... (생성자 및 다른 필드 생략)

    @Override
    public void compensateCoupon(String couponNumber, String orderId) {
        Coupon coupon = loadCouponPort.loadCoupon(couponNumber)
                .orElse(null); // 쿠폰을 찾을 수 없어도 오류 없이 처리

        // ✅ 이미 취소(AVAILABLE)되었거나, 존재하지 않는 쿠폰에 대한 보상 요청은 무시 (멱등성)
        if (coupon == null) {
            saveReservationCancelled(orderId, couponNumber); // 예약 기록은 남김
            return;
        }
        if (coupon.status() == CouponStatus.USED) {
            // 이미 사용 완료된 쿠폰은 보상 불가능 (예외 발생)
            throw new IllegalStateException("보상 불가능한 쿠폰입니다: " + coupon.couponNumber());
        }

        saveReservationCancelled(orderId, couponNumber); // 예약 기록을 CANCELLED 로 업데이트

        // ✅ RESERVED 상태의 쿠폰만 AVAILABLE 로 변경하여 보상 처리
        if (coupon.status() != CouponStatus.RESERVED) {
            return;
        }

        Coupon updated = new Coupon(
                coupon.couponNumber(),
                CouponStatus.AVAILABLE, // 상태를 사용 가능으로 변경
                coupon.issuedAt(),
                coupon.expiredAt()
        );
        saveCouponPort.save(updated);
    }

    // ... (다른 메서드 생략)
}
```
`compensateCoupon` 메서드는 쿠폰이 이미 `AVAILABLE` 상태이거나 존재하지 않는 경우에도 예외를 발생시키지 않고 정상적으로 처리합니다. 이는 여러 번의 보상 요청이 들어오더라도 시스템의 상태가 일관되게 유지되도록 하는 멱등성의 핵심 원리입니다.

---
이제 우리 시스템은 분산 환경의 장애에 대응하여 데이터 정합성을 유지할 수 있는 완전한 Saga 패턴을 갖추게 되었습니다. 하지만 서비스 장애가 너무 잦거나 특정 서비스가 오랫동안 응답이 없다면 어떻게 될까요? 다음 챕터에서는 **Istio**를 활용하여 이러한 상황에 더욱 효과적으로 대처하는 **서킷 브레이커(Circuit Breaker)** 패턴에 대해 알아봅니다.
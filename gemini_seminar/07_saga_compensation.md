# Chapter 7: Saga 보상 트랜잭션 (Compensating Transaction) 구현

## 1. 개요: 분산 트랜잭션의 완결성 - Saga 보상과 멱등성

이전 챕터에서 `order-saga-consumer`가 Kafka 이벤트를 소비하고 Saga의 상태에 따라 `confirm` 또는 `compensate` 로직을 호출하는 준비 과정을 살펴보았습니다. 본 챕터에서는 **Saga 패턴의 핵심**인 보상 트랜잭션(Compensating Transaction)의 개념을 깊이 있게 이해하고, 분산 트랜잭션 실패 시 시스템의 상태를 일관되게 되돌리는 로직을 구현합니다. 또한, 분산 환경에서 발생하는 **타이밍 이슈**와 이를 해결하기 위한 **멱등성(Idempotency)** 확보의 중요성에 대해 상세히 알아봅니다.

### 핵심 학습 목표
*   Saga 패턴의 보상 트랜잭션 개념과 필요성을 이해합니다.
*   `order-saga-consumer`가 `confirm`과 `compensate` 로직을 어떻게 분기하여 처리하는지 학습합니다.
*   타이밍 이슈(예: 보상 요청이 예약 요청보다 먼저 도착하는 경우)가 발생하는 시나리오와 `reservation` 테이블을 활용한 해결 전략을 이해합니다.
*   멱등성을 확보하기 위한 구현 기법을 학습하고 실제 코드에 적용된 사례를 살펴봅니다.

## 2. Saga 보상 트랜잭션과 멱등성

**Saga 패턴의 보상 트랜잭션:**
분산 트랜잭션은 여러 개의 로컬 트랜잭션으로 구성됩니다. 만약 이 중 어느 하나라도 실패하면, 앞서 성공했던 로컬 트랜잭션들의 변경사항을 취소하여 시스템 전체의 일관성을 유지해야 합니다. 이때 사용되는 것이 **보상 트랜잭션**입니다. 각 참여자 서비스는 자신이 수행했던 작업을 되돌리는 보상 로직을 제공해야 합니다.

예를 들어, 주문 중 쿠폰 예약은 성공했으나, 포인트 예약이 실패한 경우 `order-orchestrator`는 Saga를 `Compensating` 상태로 전환하고 이벤트를 발행합니다. `order-saga-consumer`는 이 이벤트를 받아 `coupon-service`에 이전에 예약했던 쿠폰을 **보상(취소)**하도록 요청합니다.

**멱등성(Idempotency):**
분산 시스템에서는 네트워크 지연, 재시도 등으로 인해 동일한 요청이 여러 번 전달될 수 있습니다. 멱등성은 **동일한 연산을 여러 번 수행하더라도 결과가 항상 동일하거나 시스템의 상태가 동일하게 유지됨**을 보장하는 속성입니다. Saga 보상 트랜잭션에서 멱등성 확보는 필수적입니다. 예를 들어, 이미 취소된 쿠폰을 다시 취소하라는 요청이 오더라도 오류 없이 처리되어야 합니다.

### 동시성 이슈와 `reservation` 테이블을 활용한 해결

분산 시스템에서는 메시지 처리 순서가 보장되지 않거나, 네트워크 지연으로 인해 예상치 못한 타이밍 이슈가 발생할 수 있습니다. 예를 들어, `order-orchestrator`가 `coupon-service`에 쿠폰 `reserve`를 요청했으나, 네트워크 지연으로 응답이 늦어지고 그 사이에 다른 이유로 `compensate` 이벤트가 먼저 발행되어 `compensate` 요청이 `coupon-service`에 먼저 도착하는 시나리오가 발생할 수 있습니다.

**문제 상황 (타이밍 충돌):**
```
시간
  |  OrderOrchestrator           CouponService              Outbox/Consumer
  |
  |  reserve 요청 ──────────────>  (지연/timeout)  ────────
  |  (오케스트레이터는 실패로 판단)                             |
  |  compensate 이벤트 발행 ─────────────────────────────── | ──────>     
  |                             compensate 처리   <────── | ──────
  |                             (예약 취소/복구 수행)        |
  |                             쿠폰 상태: AVAILABLE       |
  |                           (뒤늦게 예약 처리 완료) <────────
  |                             reserve 처리
  |                             쿠폰 상태: RESERVED  (문제 발생!)
  |
```
이러한 타이밍 충돌로 인해 보상이 먼저 수행되었는데, 뒤늦게 예약이 성공 처리되어 쿠폰 상태가 잘못 바뀌는 문제가 발생할 수 있습니다.

**해결 전략: `coupon_reservation` 테이블을 활용한 상태 기록**
`coupon_reservation` 테이블에 **`order_id` 기준의 예약/보상 상태를 기록**함으로써, 순서가 뒤바뀌더라도 일관되게 처리할 수 있습니다. 이는 각 서비스가 자신의 로컬 DB에 Saga의 미시적인 상태를 기록하여 멱등성을 확보하는 전략입니다.

*   **예약 요청 시:** `coupon_reservation`에 `RESERVED` 상태를 기록합니다. 이미 `CANCELLED`가 기록되어 있다면 예약을 무시합니다.
*   **보상 요청 시:** `coupon_reservation`에 `CANCELLED` 상태를 기록합니다. 이후 예약 요청이 오더라도 `CANCELLED` 상태라면 무시합니다.

**해결 이후의 타임라인:**
```
시간
  |  OrderOrchestrator           CouponService              coupon_reservation
  |
  |  reserve 요청 ──────────────>  (지연/timeout) ────────────────
  |  (오케스트레이터는 실패로 판단)                                    |
  |  compensate 이벤트 발행 ───────────────────────────────>       |
  |                             compensate 처리                   |
  |                             reservation: CANCELLED 저장       |
  |                                   (뒤늦게 예약 처리 시도) <───────
  |                             reserve() 처리
  |                               -reservation 상태 확인
  |                               -CANCELLED -> 예약 무시
  |                             쿠폰 상태: AVAILABLE 유지 (정상 처리)
  |
```

## 3. Saga 보상 트랜잭션 관련 Git 이력

Saga 보상 트랜잭션의 구현, 멱등성 확보 및 타이밍 이슈 해결과 관련된 주요 Git 커밋입니다.

| 커밋 ID | 날짜 | 주요 변경 요약 |
|---|---|---|
| `0b73be2` | 2026-01-05 | Saga 컨슈머 `confirm`, `compensate` 로직 추가 |
| `542ed97` | 2026-01-06 | `coupon-service` `confirm` API 추가 |
| `091c2a7` | 2026-01-06 | `coupon-service` `compensate` API 추가 |
| `66c93ca` | 2026-01-06 | `point-service`에도 `confirm`, `compensate` API 추가 |
| `7d9e662` | 2026-01-07 | 이미 확정한 point, coupon 중복 확정 시 오류 없이 처리 (멱등성) |
| `bfa985f` (`seminar3/12_merge_2026-01-15_fix_timing_issue.md` 참고) | 2026-01-15 | 타이밍 이슈 해결 (reservation 테이블 도입) |

**(실습 가이드: Git 커밋 확인)**
1.  `git checkout 7d9e662` 명령어로 해당 커밋 시점으로 이동하여 `coupon-service`와 `point-service`에 `confirm` 및 `compensate` API가 추가된 것을 확인해 보세요.
2.  `git checkout bfa985f` 명령어로 이동하여 `coupon_reservation` 테이블 및 이를 활용한 타이밍 이슈 해결 로직을 확인해 볼 수 있습니다.

## 4. 핵심 코드 스니펫: Saga 보상 및 멱등성 구현

### 4.1. `ProcessOrderSagaEventService` (Application Layer)

`order-saga-consumer`의 핵심 로직입니다. `orderId`와 `status`를 받아 Saga 정보를 조회하고, `sagaStatus`에 따라 `handleConfirm` 또는 `handleCompensate` 메서드를 호출합니다.

**`order-saga-consumer/src/main/java/com/example/ordersagaconsumer/application/service/ProcessOrderSagaEventService.java`**
```java
// ... imports ...
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
            System.err.println("### OrderSaga lookup skipped ### : empty orderId");
            return;
        }

        OrderSagaInfo info = loadOrderSagaPort.findByOrderId(orderId)
                .orElse(null);

        if (info == null) {
            System.err.println("### OrderSaga not found ### : orderId=" + orderId + " status=" + status);
            return;
        }

        System.out.println("### OrderSaga details ### : orderId=" + orderId + " status=" + status
                + " couponNumber=" + info.couponNumber() + " pointNumber=" + info.pointNumber());

        OrderSagaStatus sagaStatus = parseSagaStatus(status);
        if (sagaStatus == null) {
            System.err.println("### OrderSaga status skipped ### : unsupported status=" + status);
            return;
        }

        if (sagaStatus == OrderSagaStatus.Reserved) {
            handleConfirm(orderId, info); // Saga 상태가 Reserved이면 확정 로직 수행
            return;
        }

        if (sagaStatus == OrderSagaStatus.Compensating) {
            handleCompensate(orderId, info); // Saga 상태가 Compensating이면 보상 로직 수행
        }
    }

    // 외부 서비스 확정(Confirm) 로직
    private void handleConfirm(String orderId, OrderSagaInfo info) {
        boolean couponNeeded = StringUtils.hasText(info.couponNumber());
        boolean pointNeeded = StringUtils.hasText(info.pointNumber());

        boolean couponOk = true;
        boolean pointOk = true;

        if (couponNeeded) {
            couponOk = couponServicePort.confirm(info.couponNumber(), orderId); // 쿠폰 서비스 확정 호출
            updateOutboxMessagePort.updateCouponStatus(
                    orderId,
                    couponOk ? MSAStatus.Completed : MSAStatus.Failed // Outbox 상태 업데이트
            );
        }

        if (pointNeeded) {
            pointOk = pointServicePort.confirm(info.pointNumber(), orderId); // 포인트 서비스 확정 호출
            updateOutboxMessagePort.updatePointStatus(
                    orderId,
                    pointOk ? MSAStatus.Completed : MSAStatus.Failed // Outbox 상태 업데이트
            );
        }

        if (couponOk && pointOk) { // 모든 확정 로직이 성공했으면 Saga 완료
            sagaStatusTransitionService.markCompleted(orderId);
        }
    }

    // 외부 서비스 보상(Compensate) 로직
    private void handleCompensate(String orderId, OrderSagaInfo info) {
        boolean couponNeeded = StringUtils.hasText(info.couponNumber());
        boolean pointNeeded = StringUtils.hasText(info.pointNumber());

        boolean couponOk = true;
        boolean pointOk = true;

        if (couponNeeded) {
            couponOk = couponServicePort.compensate(info.couponNumber(), orderId); // 쿠폰 서비스 보상 호출
            updateOutboxMessagePort.updateCouponStatus(
                    orderId,
                    couponOk ? MSAStatus.Compensated : MSAStatus.Failed // Outbox 상태 업데이트
            );
        }

        if (pointNeeded) {
            pointOk = pointServicePort.compensate(info.pointNumber(), orderId); // 포인트 서비스 보상 호출
            updateOutboxMessagePort.updatePointStatus(
                    orderId,
                    pointOk ? MSAStatus.Compensated : MSAStatus.Failed // Outbox 상태 업데이트
            );
        }

        if (couponOk && pointOk) { // 모든 보상 로직이 성공했으면 Saga 보상 완료
            sagaStatusTransitionService.markCompensated(orderId);
        }
    }

    // 문자열 상태를 OrderSagaStatus enum으로 파싱
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

### 4.2. `coupon-service`의 멱등성 보상 로직 (`ReserveCouponService`)

`coupon-service`의 `ReserveCouponService`는 `confirm` 및 `compensate` 메서드에서 `CouponReservation` 테이블을 활용하여 멱등성을 확보하고 타이밍 이슈를 해결합니다.

**`coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponService.java`**
```java
// ... imports ...
@Service
@RequiredArgsConstructor
@Transactional
public class ReserveCouponService implements ReserveCouponUseCase, ConfirmCouponUseCase, CompensateCouponUseCase {

    private final LoadCouponPort loadCouponPort;
    private final SaveCouponPort saveCouponPort;
    private final LoadCouponReservationPort loadCouponReservationPort; // CouponReservation 관리 Output Port
    private final SaveCouponReservationPort saveCouponReservationPort; // CouponReservation 관리 Output Port

    @Override
    public void reserve(String couponNumber, String orderId) {
        // [타이밍 이슈 해결 로직]
        if (isReservationCancelled(orderId)) { // 1. 이미 보상 처리된 주문이면 예약을 무시 (멱등성)
            System.out.println("### Coupon Reserve Skipped ### : Already cancelled orderId=" + orderId);
            return;
        }
        verifyReservationNotAlreadyReserved(orderId); // 2. 이미 예약된 주문이면 예외 발생 (멱등성)

        Coupon coupon = loadCouponPort.loadCoupon(couponNumber)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다: " + couponNumber));

        if (!coupon.isAvailable()) {
            throw new IllegalStateException("예약 불가능한 쿠폰입니다: " + couponNumber);
        }

        // 쿠폰 상태를 RESERVED로 변경
        Coupon reservedCoupon = new Coupon(
                coupon.couponNumber(),
                CouponStatus.RESERVED,
                coupon.issuedAt(),
                coupon.expiredAt()
        );
        saveCouponPort.save(reservedCoupon);

        // [타이밍 이슈 해결 로직] 쿠폰 예약 정보 저장 (RESERVED 상태)
        saveCouponReservationPort.saveReservation(new CouponReservation(
                orderId,
                couponNumber,
                ReservationStatus.RESERVED
        ));
        System.out.println("### Coupon Reserved ### : orderId=" + orderId + ", couponNumber=" + couponNumber);
    }

    @Override
    public void confirm(String couponNumber, String orderId) {
        // [멱등성] 이미 사용 완료된 쿠폰이면 다시 확정하지 않음
        Coupon coupon = loadCouponPort.loadCoupon(couponNumber)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다: " + couponNumber));
        if (coupon.status() == CouponStatus.USED) {
            System.out.println("### Coupon Confirm Skipped ### : Already USED couponNumber=" + couponNumber);
            return;
        }
        validateConfirmable(coupon); // RESERVED 상태인지 확인

        // 쿠폰 상태를 USED로 변경
        Coupon usedCoupon = new Coupon(
                coupon.couponNumber(),
                CouponStatus.USED,
                coupon.issuedAt(),
                coupon.expiredAt()
        );
        saveCouponPort.save(usedCoupon);
        System.out.println("### Coupon Confirmed ### : orderId=" + orderId + ", couponNumber=" + couponNumber);
    }

    @Override
    public void compensateCoupon(String couponNumber, String orderId) {
        // [타이밍 이슈 해결 로직]
        saveReservationCancelled(orderId, couponNumber); // 1. 예약 취소 정보 저장

        Coupon coupon = loadCouponPort.loadCoupon(couponNumber)
                .orElse(null);
        if (coupon == null) { // 쿠폰이 없으면 이미 취소된 것으로 간주
            System.out.println("### Coupon Compensate Skipped ### : Coupon not found, assuming already compensated. orderId=" + orderId);
            return;
        }
        if (coupon.status() == CouponStatus.USED) {
            // 이미 사용된 쿠폰은 보상 불가능 (예외 발생 또는 오류 로그)
            System.err.println("### Coupon Compensate Failed ### : Not compensatable - Already USED couponNumber=" + couponNumber);
            throw new IllegalStateException("보상 불가능한 쿠폰입니다: " + coupon.couponNumber());
        }
        if (coupon.status() != CouponStatus.RESERVED) { // RESERVED 상태가 아니면 보상 불필요
            System.out.println("### Coupon Compensate Skipped ### : Not in RESERVED state. couponNumber=" + couponNumber);
            return;
        }

        // 쿠폰 상태를 AVAILABLE로 되돌림
        Coupon availableCoupon = new Coupon(
                coupon.couponNumber(),
                CouponStatus.AVAILABLE,
                coupon.issuedAt(),
                coupon.expiredAt()
        );
        saveCouponPort.save(availableCoupon);
        System.out.println("### Coupon Compensated ### : orderId=" + orderId + ", couponNumber=" + couponNumber);
    }

    // ... (isReservationCancelled, verifyReservationNotAlreadyReserved, saveReservationCancelled 등 헬퍼 메서드 생략) ...
}
```
**`coupon-service/src/main/java/com/example/couponservice/domain/model/CouponReservation.java`**
```java
// ... imports ...
public class CouponReservation {
    private final String orderId;
    private final String couponNumber;
    private final ReservationStatus status; // RESERVED 또는 CANCELLED

    // ... (생성자, getter 생략) ...
}
```
**`coupon-service/src/main/java/com/example/couponservice/domain/model/status/ReservationStatus.java`**
```java
package com.example.couponservice.domain.model.status;

public enum ReservationStatus {
    RESERVED,
    CANCELLED
}
```
**설명:**
*   `reserve` 메서드 초반에 `isReservationCancelled(orderId)`와 `verifyReservationNotAlreadyReserved(orderId)`를 통해 이미 보상되었거나 예약된 요청에 대해 멱등성을 확보합니다.
*   `compensateCoupon` 메서드에서는 `saveReservationCancelled(orderId, couponNumber)`를 통해 `coupon_reservation` 테이블에 `CANCELLED` 상태를 먼저 기록하여, 이후 동일한 `orderId`에 대한 `reserve` 요청이 도착하더라도 이를 무시하도록 합니다.
*   `confirm` 메서드 역시 `coupon.status() == CouponStatus.USED`인 경우 즉시 반환하여 멱등성을 보장합니다.

## 5. 실습 체크포인트

### 5.1. Saga 보상 트랜잭션 시뮬레이션
`order-orchestrator`에서 외부 서비스 호출 중 실패를 유도하여 보상 트랜잭션이 발생하는 시나리오를 시뮬레이션합니다.

1.  **필수 서비스 실행:**
    *   Chapter 4에서 배포한 Kafka 클러스터가 실행 중인지 확인합니다.
    *   `coupon-service`, `point-service`, `order-orchestrator`, `order-saga-consumer`가 모두 실행 중인지 확인합니다. (Chapter 6 실습 가이드 참조)
2.  **포인트 서비스 실패 유도:**
    *   `point-service`의 `ReservePointService.reserve` 메서드에서 특정 `pointNumber` (예: "PNT-FAIL-001")에 대해 항상 `new IllegalStateException("강제 포인트 예약 실패")`를 발생시키도록 코드를 임시로 수정합니다.
3.  **`order-orchestrator`를 통한 주문 생성:**
    *   `order-orchestrator/src/test/httprequest/01_orderOrchestratorTest.http` 파일에서 "주문 생성 요청"을 사용하여 API를 호출합니다. 이때, 실패를 유도할 `pointNumber`를 포함해야 합니다.
    *   **요청 바디 예시:**
        ```json
        {
          "couponNumber": "CPN-INT-AVAILABLE-001",
          "pointNumber": "PNT-FAIL-001", // 실패를 유도할 포인트 번호
          "paymentNumber": "PAY-001",
          "paymentAmount": 35000,
          "orderItems": [
            {
              "itemNumber": "ITEM-001",
              "quantity": 2
            }
          ]
        }
        ```
    *   **예상 결과:**
        *   `order-orchestrator`는 포인트 예약 실패를 감지하고, Saga 상태를 `Compensating`으로 변경한 후 `RESERVE_FAILED` 이벤트를 Kafka에 발행합니다.
        *   `order-saga-consumer`는 `Compensating` 이벤트를 수신하고, `handleCompensate` 로직을 호출합니다.
        *   `handleCompensate`는 `coupon-service`에 이전에 예약된 쿠폰을 보상하도록 요청합니다.
        *   각 서비스의 로그에서 `Compensating` 관련 메시지(`### Coupon Compensated ###`)와 Saga 상태 전이(`markCompensated`)를 확인할 수 있습니다.
4.  **H2 Console을 통해 DB 확인:**
    *   `order-orchestrator`의 H2 DB에서 `SELECT * FROM ORDER_SAGA;`를 실행하여 Saga 상태가 `Compensated`로 변경되었는지 확인합니다.
    *   `coupon-service`의 H2 DB에서 `SELECT * FROM COUPON;`을 실행하여 `CPN-INT-AVAILABLE-001` 쿠폰의 상태가 다시 `AVAILABLE`로 되돌아왔는지 확인합니다.
    *   `coupon-service`의 H2 DB에서 **`SELECT * FROM COUPON_RESERVATION;`**를 실행하여 `orderId`에 해당하는 `ReservationStatus`가 `CANCELLED`로 기록되었는지 확인합니다.

---
Saga 보상 트랜잭션과 멱등성 구현을 통해 우리는 분산 시스템의 최종 일관성을 보장할 수 있게 되었습니다. 이제 다음 챕터에서는 시스템의 외부적인 장애 상황에 대한 복원력을 강화하기 위해 **Istio Circuit Breaker**를 어떻게 적용하는지 알아봅니다.
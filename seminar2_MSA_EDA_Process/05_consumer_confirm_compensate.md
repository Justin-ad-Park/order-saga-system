# 05. 컨슈머: confirm / compensate

## 목표
컨슈머가 confirm/compensate를 어떻게 분기하고 쿠폰/포인트 호출로 이어지는지 이해한다.

## 핵심 흐름
- 이벤트에서 saga status 읽기  
- Reserved -> confirm  
- Compensating -> compensate

## 컨슈머 처리 로직
`order-saga-consumer/src/main/java/com/example/ordersagaconsumer/application/service/ProcessOrderSagaEventService.java`
```java
@Service
public class ProcessOrderSagaEventService implements ProcessOrderSagaEventUseCase {

    @Override
    public void process(String orderId, String status) {
        if (orderId == null || orderId.isBlank()) {
            return;
        }

        OrderSagaInfo info = loadOrderSagaPort.findByOrderId(orderId)
                .orElse(null);
        if (info == null) {
            return;
        }

        OrderSagaStatus sagaStatus = parseSagaStatus(status);
        if (sagaStatus == null) {
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
}
```

## 쿠폰 confirm/compensate
`coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponService.java`
```java
@Service
@Transactional
public class ReserveCouponService implements ReserveCouponUseCase, ConfirmCouponUseCase, CompensateCouponUseCase {

    @Override
    public void confirm(String couponNumber, String orderId) {
        Coupon coupon = loadCouponPort.loadCoupon(couponNumber)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found: " + couponNumber));
        if (coupon.status() == CouponStatus.USED) {
            return;
        }
        validateConfirmable(coupon);

        Coupon updated = new Coupon(
                coupon.couponNumber(),
                CouponStatus.USED,
                coupon.issuedAt(),
                coupon.expiredAt()
        );
        saveCouponPort.save(updated);
    }

    @Override
    public void compensateCoupon(String couponNumber, String orderId) {
        Coupon coupon = loadCouponPort.loadCoupon(couponNumber)
                .orElse(null);
        if (coupon == null) {
            saveReservationCancelled(orderId, couponNumber);
            return;
        }
        if (coupon.status() == CouponStatus.USED) {
            throw new IllegalStateException("Not compensatable: " + coupon.couponNumber());
        }

        saveReservationCancelled(orderId, couponNumber);
        if (coupon.status() != CouponStatus.RESERVED) {
            return;
        }

        Coupon updated = new Coupon(
                coupon.couponNumber(),
                CouponStatus.AVAILABLE,
                coupon.issuedAt(),
                coupon.expiredAt()
        );
        saveCouponPort.save(updated);
    }
}
```

## 포인트 confirm/compensate
`point-service/src/main/java/com/example/pointservice/application/service/ReservePointService.java`
```java
@Service
@Transactional
public class ReservePointService implements ReservePointUseCase, ConfirmPointUseCase, CompensatePointUseCase {

    @Override
    public void confirm(String pointNumber, String orderId) {
        Point point = loadPointPort.loadPoint(pointNumber)
                .orElseThrow(() -> new IllegalArgumentException("Point not found: " + pointNumber));
        if (point.status() == PointStatus.USED) {
            return;
        }
        validateConfirmable(point);

        Point updated = new Point(
                point.pointNumber(),
                PointStatus.USED,
                point.issuedAt(),
                point.expiredAt()
        );
        savePointPort.save(updated);
    }

    @Override
    public void compensatePoint(String pointNumber, String orderId) {
        Point point = loadPointPort.loadPoint(pointNumber)
                .orElse(null);
        if (point == null) {
            saveReservationCancelled(orderId, pointNumber);
            return;
        }
        if (point.status() == PointStatus.USED) {
            throw new IllegalStateException("Not compensatable: " + point.pointNumber());
        }

        saveReservationCancelled(orderId, pointNumber);
        if (point.status() != PointStatus.RESERVED) {
            return;
        }

        Point updated = new Point(
                point.pointNumber(),
                PointStatus.AVAILABLE,
                point.issuedAt(),
                point.expiredAt()
        );
        savePointPort.save(updated);
    }
}
```

## 실습 체크포인트
- 컨슈머 실행 후 쿠폰/포인트 상태 전이 확인
- 사가 상태가 Completed 또는 Compensated로 전환되는지 확인

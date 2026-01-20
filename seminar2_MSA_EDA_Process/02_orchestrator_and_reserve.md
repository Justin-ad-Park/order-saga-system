# 02. 오케스트레이터 + 분산 예약

## 목표
오케스트레이터가 분산 호출을 묶어 사가를 시작하는 방식을 이해한다.

## 핵심 흐름
1) 주문/사가 생성  
2) 쿠폰/포인트 예약 병렬 호출  
3) 사가 상태 업데이트 + 이벤트 발행

## 오케스트레이터 진입점
`order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`
```java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderOrchestrationController {

    private final CreateOrderUseCase createOrderUseCase;
    private final ReserveExternalResourcesService reserveExternalResourcesService;
    private final UpdateOutboxMessageUseCase updateOutboxMessageUseCase;
    private final UpdateOrderSagaStatusUseCase updateOrderSagaStatusUseCase;
    private final OrderSagaEventService orderSagaEventService;

    @PostMapping
    public Mono<ResponseEntity<CreateOrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        CreateOrderCommand command = mapToCommand(request);
        CreateOrderResult result = createOrderUseCase.createOrder(command);

        return reserveExternalResourcesService.reserveExternalResources(
                        result.orderId(),
                        request.couponNumber(),
                        request.pointNumber()
                )
                .then(Mono.fromRunnable(() -> {
                    updateSagaStatus(result.orderId(), OrderSagaStatus.Reserved);
                    publishSagaEvent(result, OrderSagaStatus.Reserved, OrderSagaEventType.RESERVE_SUCCEEDED);
                }))
                .onErrorResume(ex -> {
                    updateSagaStatus(result.orderId(), OrderSagaStatus.Compensating);
                    publishSagaEvent(result, OrderSagaStatus.Compensating, OrderSagaEventType.RESERVE_FAILED);
                    return Mono.error(ex);
                })
                .thenReturn(ResponseEntity.ok(mapToResponse(result)));
    }

    private void updateSagaStatus(String orderId, OrderSagaStatus status) {
        updateOrderSagaStatusUseCase.updateStatus(orderId, status);
        updateOutboxMessageUseCase.updateSagaStatus(orderId, status);
    }

    private void publishSagaEvent(CreateOrderResult result, OrderSagaStatus status, OrderSagaEventType type) {
        orderSagaEventService.publish(result.orderId(), result.sagaId(), status, type);
    }
}
```

## 분산 예약 처리
`order-orchestrator/src/main/java/com/example/orderorchestrator/application/service/ReserveExternalResourcesService.java`
```java
@Service
@RequiredArgsConstructor
public class ReserveExternalResourcesService {

    private final ReserveCouponPort reserveCouponPort;
    private final ReservePointPort reservePointPort;
    private final UpdateOutboxMessageUseCase updateOutboxMessageUseCase;

    public Mono<Void> reserveExternalResources(String orderId, String couponNumber, String pointNumber) {
        List<Mono<?>> calls = new ArrayList<>();
        if (StringUtils.hasText(couponNumber)) {
            calls.add(reserveCoupon(couponNumber, orderId));
        }
        if (StringUtils.hasText(pointNumber)) {
            calls.add(reservePoint(pointNumber, orderId));
        }
        if (calls.isEmpty()) {
            return Mono.empty();
        }
        return Mono.whenDelayError(calls).then();
    }

    private Mono<Void> reserveCoupon(String couponNumber, String orderId) {
        return reserveCouponPort.reserveCoupon(couponNumber, orderId)
                .doOnSuccess(ignored -> updateOutboxMessageUseCase.updateCouponStatus(orderId, MSAStatus.Reserved))
                .onErrorResume(ex -> {
                    updateOutboxMessageUseCase.updateCouponStatus(orderId, MSAStatus.Failed);
                    return Mono.error(ex);
                })
                .then();
    }

    private Mono<Void> reservePoint(String pointNumber, String orderId) {
        return reservePointPort.reservePoint(pointNumber, orderId)
                .doOnSuccess(ignored -> updateOutboxMessageUseCase.updatePointStatus(orderId, MSAStatus.Reserved))
                .onErrorResume(ex -> {
                    updateOutboxMessageUseCase.updatePointStatus(orderId, MSAStatus.Failed);
                    return Mono.error(ex);
                })
                .then();
    }
}
```

## 쿠폰 Reserve 처리 흐름
`coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`
```java
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {
    private final ReserveCouponUseCase reserveCouponUseCase;

    @PostMapping("/reserve")
    public ApiResponse<ReserveCouponResponse> reserveCoupon(@RequestBody ReserveCouponRequest request) {
        reserveCouponUseCase.reserve(request.couponNumber(), request.orderId());
        return ApiResponse.ok(ReserveCouponResponse.of(request.couponNumber(), request.orderId()));
    }
}
```

`coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponService.java`
```java
@Service
@RequiredArgsConstructor
@Transactional
public class ReserveCouponService implements ReserveCouponUseCase, ConfirmCouponUseCase, CompensateCouponUseCase {

    @Override
    public void reserve(String couponNumber, String orderId) {
        if (isReservationCancelled(orderId)) {
            return;
        }
        verifyReservationNotAlreadyReserved(orderId);
        updateStatus(couponNumber, CouponStatus.RESERVED, this::validateReservable);
        saveCouponReservationPort.saveReservation(new CouponReservation(
                orderId,
                couponNumber,
                ReservationStatus.RESERVED
        ));
    }
}
```

## 실습 체크포인트
- API 호출: `POST /api/v1/orders` (coupon/point 포함)
- 기대 결과: 사가 상태가 Reserved 또는 Compensating으로 이동

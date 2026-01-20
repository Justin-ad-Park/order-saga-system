# 02. Orchestrator + distributed reservation

## Goal
Explain how the orchestrator coordinates distributed calls and starts a saga.

## Core flow
1) Create order + saga record
2) Call coupon/point reservation in parallel
3) Update saga status and emit event

## Orchestrator entry point
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

## Distributed reservation
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

## Hands-on checkpoints
- Call API: `POST /api/v1/orders` with coupon/point
- Expect saga status to move to Reserved or Compensating

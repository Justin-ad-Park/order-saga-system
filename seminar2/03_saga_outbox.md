# 03. Saga creation + Outbox persistence

## Goal
Show how order creation and outbox write happen in a single transaction, preparing event delivery.

## Core flow
- Generate order/saga IDs
- Persist OrderSaga
- Persist OutboxMessage

## Create order and write outbox
`order-orchestrator/src/main/java/com/example/orderorchestrator/application/service/CreateOrderService.java`
```java
@Service
@Transactional
public class CreateOrderService implements CreateOrderUseCase {

    private final SaveOrderSagaPort saveOrderSagaPort;
    private final SaveOutboxMessagePort saveOutboxMessagePort;

    @Override
    public CreateOrderResult createOrder(CreateOrderCommand command) {
        String orderId = "ORD-" + createUuid();
        String sagaId = "SAGA-" + createUuid();

        List<OrderItem> items = command.orderItems().stream()
                .map(i -> new OrderItem(i.itemNumber(), i.quantity()))
                .collect(Collectors.toList());

        OrderSaga saga = OrderSaga.create(
                orderId,
                sagaId,
                command.couponNumber(),
                command.pointNumber(),
                command.paymentNumber(),
                command.paymentAmount(),
                items,
                OrderSagaStatus.InProgress
        );

        OrderSaga savedSaga = saveOrderSagaPort.save(saga);

        MSAStatus couponStatus = resolveUsageStatus(command.couponNumber());
        MSAStatus pointStatus = resolveUsageStatus(command.pointNumber());

        OutboxMessage message = OutboxMessage.initial(
                savedSaga.orderId(),
                "{}",
                couponStatus,
                pointStatus
        );

        saveOutboxMessagePort.save(message);

        return CreateOrderResult.of(
                savedSaga.orderId(),
                savedSaga.sagaId(),
                savedSaga.status().name()
        );
    }
}
```

## Outbox write + status updates
`order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/OutboxMessagePersistenceAdapter.java`
```java
@Repository
@Transactional
public class OutboxMessagePersistenceAdapter implements SaveOutboxMessagePort, UpdateOutboxMessagePort {

    private final OutboxMessageJpaRepository outboxMessageJpaRepository;

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
    public void updateSagaStatus(String orderId, OrderSagaStatus status) {
        int updated = outboxMessageJpaRepository.updateSagaStatus(orderId, status, LocalDateTime.now());
        if (updated == 0) {
            throw new IllegalArgumentException("Outbox message not found: " + orderId);
        }
    }
}
```

## Hands-on checkpoints
- Verify: `order_saga`, `outbox_message` rows after order creation
- Confirm saga status and outbox status changes

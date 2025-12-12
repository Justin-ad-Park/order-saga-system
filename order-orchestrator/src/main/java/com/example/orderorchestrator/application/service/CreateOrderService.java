package com.example.orderorchestrator.application.service;

import com.example.orderorchestrator.application.port.in.command.CreateOrderCommand;
import com.example.orderorchestrator.application.port.in.result.CreateOrderResult;
import com.example.orderorchestrator.application.port.in.CreateOrderUseCase;
import com.example.orderorchestrator.application.port.out.SaveOrderSagaPort;
import com.example.orderorchestrator.application.port.out.SaveOutboxMessagePort;
import com.example.orderorchestrator.domain.model.OrderItem;
import com.example.orderorchestrator.domain.model.OrderSaga;
import com.example.orderorchestrator.domain.model.status.OrderSagaStatus;
import com.example.orderorchestrator.domain.outbox.OutboxMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.example.orderorchestrator.application.service.uuid.UUIDGenerator.createUuid;

@Service
@Transactional
public class CreateOrderService implements CreateOrderUseCase {

    private final SaveOrderSagaPort saveOrderSagaPort;
    private final SaveOutboxMessagePort saveOutboxMessagePort;

    public CreateOrderService(
            SaveOrderSagaPort saveOrderSagaPort,
            SaveOutboxMessagePort saveOutboxMessagePort
    ) {
        this.saveOrderSagaPort = saveOrderSagaPort;
        this.saveOutboxMessagePort = saveOutboxMessagePort;
    }

    @Override
    public CreateOrderResult createOrder(CreateOrderCommand command) {
        // 1) 주문ID / SagaID 생성 (임시: UUID 기반)
        String orderId = "ORD-" + createUuid();
        String sagaId = "SAGA-" + createUuid();

        // 2) Command → 도메인 OrderItem 리스트 변환
        List<OrderItem> items = command.orderItems().stream()
                .map(i -> new OrderItem(i.itemNumber(), i.quantity()))
                .collect(Collectors.toList());

        // 3) OrderSaga 엔티티 생성 (초기 상태: InProgress)
        OrderSaga saga = OrderSaga.create(
                orderId,
                sagaId,
                command.couponNumber(),
                command.paymentNumber(),
                command.paymentAmount(),
                items,
                OrderSagaStatus.InProgress   // ✅ 변경된 enum 사용
        );

        // 4) Saga 저장
        OrderSaga savedSaga = saveOrderSagaPort.save(saga);

        // 5) Outbox 메시지 생성 (payload는 우선 빈 JSON으로 두고, 나중에 스키마 설계)
        OutboxMessage message = OutboxMessage.initial(
                savedSaga.orderId(),   // ✅ 새 구조: orderId만 전달
                "{}"                   // payload (TODO: 실제 JSON으로 교체)
        );

        // 6) Outbox 저장
        saveOutboxMessagePort.save(message);

        // 7) 결과 반환
        return CreateOrderResult.of(
                savedSaga.orderId(),
                savedSaga.sagaId(),
                savedSaga.status().name()  // OrderSagaStatus → String
        );
    }


}
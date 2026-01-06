package com.example.orderorchestrator.application.service;

import com.example.orderorchestrator.application.port.in.command.CreateOrderCommand;
import com.example.orderorchestrator.application.port.in.result.CreateOrderResult;
import com.example.orderorchestrator.application.port.out.SaveOrderSagaPort;
import com.example.orderorchestrator.application.port.out.SaveOutboxMessagePort;
import com.example.orderorchestrator.domain.model.OrderItem;
import com.example.orderorchestrator.domain.model.OrderSaga;
import com.example.common.status.MSAStatus;
import com.example.common.status.OrderSagaStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CreateOrderServiceTest {

    private SaveOrderSagaPort saveOrderSagaPort;
    private SaveOutboxMessagePort saveOutboxMessagePort;

    private CreateOrderService createOrderService;

    @BeforeEach
    void setUp() {
        saveOrderSagaPort = mock(SaveOrderSagaPort.class);
        saveOutboxMessagePort = mock(SaveOutboxMessagePort.class);

        createOrderService = new CreateOrderService(
                saveOrderSagaPort,
                saveOutboxMessagePort
        );
    }

    @Test
    void createOrder_shouldCreateOrderSagaAndOutboxMessage() {
        // given
        CreateOrderCommand command = new CreateOrderCommand(
                "CPN-SVC-001",
                "PNT-SVC-001",
                "PAY-001",
                35000L,
                List.of(
                        new CreateOrderCommand.OrderItemCommand("ITEM-001", 2),
                        new CreateOrderCommand.OrderItemCommand("ITEM-002", 1)
                )
        );

        OrderSaga savedSaga = OrderSaga.create(
                "ORD-20250101-000001",
                "SAGA-20250101-000001",
                command.couponNumber(),
                command.pointNumber(),
                command.paymentNumber(),
                command.paymentAmount(),
                List.of(
                        new OrderItem("ITEM-001", 2),
                        new OrderItem("ITEM-002", 1)
                ),
                OrderSagaStatus.InProgress
        );

        when(saveOrderSagaPort.save(any(OrderSaga.class)))
                .thenReturn(savedSaga);

        // when
        CreateOrderResult result = createOrderService.createOrder(command);

        // then
        assertThat(result.orderId()).isEqualTo("ORD-20250101-000001");
        assertThat(result.sagaId()).isEqualTo("SAGA-20250101-000001");
        Assertions.assertEquals(result.status(), OrderSagaStatus.InProgress.toString());

        // OrderSaga 저장 확인
        verify(saveOrderSagaPort, times(1)).save(any(OrderSaga.class));

        // OutboxMessage 저장 확인 (ORDER_CREATED, PENDING 상태)
        verify(saveOutboxMessagePort, times(1))
                .save(argThat(outbox ->
                        outbox.orderId().equals("ORD-20250101-000001")
                                && outbox.couponStatus() == MSAStatus.InProgress
                                && outbox.pointStatus() == MSAStatus.InProgress
                                && outbox.orderStatus() == MSAStatus.InProgress
                                && outbox.sagaStatus() == OrderSagaStatus.InProgress
                ));
    }
}

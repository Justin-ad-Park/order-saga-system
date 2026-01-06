package com.example.orderorchestrator.application.port.in;

import com.example.common.status.OrderSagaStatus;

public interface UpdateOrderSagaStatusUseCase {
    void updateStatus(String orderId, OrderSagaStatus status);
}

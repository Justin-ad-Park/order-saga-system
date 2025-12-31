package com.example.orderorchestrator.application.port.out;

import com.example.orderorchestrator.domain.model.status.OrderSagaStatus;

public interface UpdateOrderSagaStatusPort {
    void updateStatus(String orderId, OrderSagaStatus status);
}

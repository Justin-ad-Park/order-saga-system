package com.example.orderorchestrator.application.port.out;

import com.example.common.status.OrderSagaStatus;

public interface UpdateOrderSagaStatusPort {
    void updateStatus(String orderId, OrderSagaStatus status);
}

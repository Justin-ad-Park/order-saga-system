package com.example.ordersagaconsumer.application.port.out;

import com.example.ordersagaconsumer.domain.model.status.OrderSagaStatus;

public interface UpdateOrderSagaStatusPort {
    void updateStatus(String orderId, OrderSagaStatus status);
}

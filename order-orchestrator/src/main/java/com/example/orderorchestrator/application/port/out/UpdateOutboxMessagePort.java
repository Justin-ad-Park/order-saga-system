package com.example.orderorchestrator.application.port.out;

import com.example.orderorchestrator.domain.model.status.MSAStatus;
import com.example.orderorchestrator.domain.model.status.OrderSagaStatus;

public interface UpdateOutboxMessagePort {
    void updateCouponStatus(String orderId, MSAStatus status);
    void updatePointStatus(String orderId, MSAStatus status);
    void updateSagaStatus(String orderId, OrderSagaStatus status);
}

package com.example.orderorchestrator.application.port.in;

import com.example.common.status.MSAStatus;
import com.example.common.status.OrderSagaStatus;

public interface UpdateOutboxMessageUseCase {
    void updateCouponStatus(String orderId, MSAStatus status);
    void updatePointStatus(String orderId, MSAStatus status);
    void updateSagaStatus(String orderId, OrderSagaStatus status);
}

package com.example.orderorchestrator.application.port.out;

import com.example.common.status.MSAStatus;
import com.example.common.status.OrderSagaStatus;

public interface UpdateOutboxMessagePort {
    void updateCouponStatus(String orderId, MSAStatus status);
    void updatePointStatus(String orderId, MSAStatus status);
    void updateSagaStatus(String orderId, OrderSagaStatus status);
}

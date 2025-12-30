package com.example.orderorchestrator.application.port.out;

import com.example.orderorchestrator.domain.model.status.MSAStatus;

public interface UpdateOutboxMessagePort {
    void updateCouponStatus(String orderId, MSAStatus status);
    void updatePointStatus(String orderId, MSAStatus status);
}

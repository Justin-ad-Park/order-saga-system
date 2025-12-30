package com.example.orderorchestrator.application.port.in;

import com.example.orderorchestrator.domain.model.status.MSAStatus;

public interface UpdateOutboxMessageUseCase {
    void updateCouponStatus(String orderId, MSAStatus status);
    void updatePointStatus(String orderId, MSAStatus status);
}

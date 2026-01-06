package com.example.orderorchestrator.application.service;

import com.example.orderorchestrator.application.port.in.UpdateOutboxMessageUseCase;
import com.example.orderorchestrator.application.port.out.UpdateOutboxMessagePort;
import com.example.common.status.MSAStatus;
import com.example.common.status.OrderSagaStatus;
import org.springframework.stereotype.Service;

@Service
public class UpdateOutboxMessageService implements UpdateOutboxMessageUseCase {

    private final UpdateOutboxMessagePort updateOutboxMessagePort;

    public UpdateOutboxMessageService(UpdateOutboxMessagePort updateOutboxMessagePort) {
        this.updateOutboxMessagePort = updateOutboxMessagePort;
    }

    @Override
    public void updateCouponStatus(String orderId, MSAStatus status) {
        updateOutboxMessagePort.updateCouponStatus(orderId, status);
    }

    @Override
    public void updatePointStatus(String orderId, MSAStatus status) {
        updateOutboxMessagePort.updatePointStatus(orderId, status);
    }

    @Override
    public void updateSagaStatus(String orderId, OrderSagaStatus status) {
        updateOutboxMessagePort.updateSagaStatus(orderId, status);
    }
}

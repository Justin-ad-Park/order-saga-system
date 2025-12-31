package com.example.orderorchestrator.application.service;

import com.example.orderorchestrator.application.port.in.UpdateOrderSagaStatusUseCase;
import com.example.orderorchestrator.application.port.out.UpdateOrderSagaStatusPort;
import com.example.orderorchestrator.domain.model.status.OrderSagaStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateOrderSagaStatusService implements UpdateOrderSagaStatusUseCase {

    private final UpdateOrderSagaStatusPort updateOrderSagaStatusPort;

    public UpdateOrderSagaStatusService(UpdateOrderSagaStatusPort updateOrderSagaStatusPort) {
        this.updateOrderSagaStatusPort = updateOrderSagaStatusPort;
    }

    @Override
    public void updateStatus(String orderId, OrderSagaStatus status) {
        updateOrderSagaStatusPort.updateStatus(orderId, status);
    }
}

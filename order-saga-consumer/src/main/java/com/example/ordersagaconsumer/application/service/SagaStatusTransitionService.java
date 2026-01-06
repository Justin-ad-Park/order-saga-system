package com.example.ordersagaconsumer.application.service;

import com.example.common.status.OrderSagaStatus;
import com.example.ordersagaconsumer.application.port.out.UpdateOrderSagaStatusPort;
import com.example.ordersagaconsumer.application.port.out.UpdateOutboxMessagePort;
import org.springframework.stereotype.Service;

@Service
public class SagaStatusTransitionService {

    private final UpdateOutboxMessagePort updateOutboxMessagePort;
    private final UpdateOrderSagaStatusPort updateOrderSagaStatusPort;

    public SagaStatusTransitionService(
            UpdateOutboxMessagePort updateOutboxMessagePort,
            UpdateOrderSagaStatusPort updateOrderSagaStatusPort
    ) {
        this.updateOutboxMessagePort = updateOutboxMessagePort;
        this.updateOrderSagaStatusPort = updateOrderSagaStatusPort;
    }

    public void markCompleted(String orderId) {
        updateOutboxMessagePort.updateCompletedStatus(orderId);
        updateOrderSagaStatusPort.updateStatus(orderId, OrderSagaStatus.Completed);
    }

    public void markCompensated(String orderId) {
        updateOutboxMessagePort.updateCompensatedStatus(orderId);
        updateOrderSagaStatusPort.updateStatus(orderId, OrderSagaStatus.Compensated);
    }
}

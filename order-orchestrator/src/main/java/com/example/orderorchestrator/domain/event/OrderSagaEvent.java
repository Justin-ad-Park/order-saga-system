package com.example.orderorchestrator.domain.event;

import com.example.orderorchestrator.domain.model.status.OrderSagaStatus;

public record OrderSagaEvent(
        String orderId,
        String sagaId,
        OrderSagaEventType type,
        OrderSagaStatus status
) {
}

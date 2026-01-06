package com.example.orderorchestrator.domain.event;

import com.example.common.status.OrderSagaStatus;

public record OrderSagaEvent(
        String orderId,
        String sagaId,
        OrderSagaEventType type,
        OrderSagaStatus status
) {
}

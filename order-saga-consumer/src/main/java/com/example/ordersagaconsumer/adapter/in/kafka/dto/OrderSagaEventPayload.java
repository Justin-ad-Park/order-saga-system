package com.example.ordersagaconsumer.adapter.in.kafka.dto;

public record OrderSagaEventPayload(
        String orderId,
        String sagaId,
        String type,
        String status
) {
}

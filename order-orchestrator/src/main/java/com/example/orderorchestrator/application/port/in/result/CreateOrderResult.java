package com.example.orderorchestrator.application.port.in.result;

public record CreateOrderResult(
        String orderId,
        String sagaId,
        String status
) {
    public static CreateOrderResult of(String orderId, String sagaId, String status) {
        return new CreateOrderResult(orderId, sagaId, status);
    }
}
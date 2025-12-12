package com.example.orderorchestrator.adapter.in.web.dto.response;

public record CreateOrderResponse(
        String orderId,
        String sagaId,
        String status
) {
    public static CreateOrderResponse of(String orderId, String sagaId, String status) {
        return new CreateOrderResponse(orderId, sagaId, status);
    }
}

package com.example.orderorchestrator.adapter.out.webclient.dto;

public record ReservePointRequest(
        String pointNumber,
        String orderId
) {
}

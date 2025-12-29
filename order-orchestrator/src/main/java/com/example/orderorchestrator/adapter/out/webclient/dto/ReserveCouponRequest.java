package com.example.orderorchestrator.adapter.out.webclient.dto;

public record ReserveCouponRequest(
        String couponNumber,
        String orderId
) {
}

package com.example.ordersagaconsumer.adapter.out.webclient.dto;

public record CompensateCouponRequest(
        String couponNumber,
        String orderId
) {
}

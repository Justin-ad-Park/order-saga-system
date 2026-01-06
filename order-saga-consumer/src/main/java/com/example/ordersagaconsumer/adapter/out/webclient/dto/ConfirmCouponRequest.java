package com.example.ordersagaconsumer.adapter.out.webclient.dto;

public record ConfirmCouponRequest(
        String couponNumber,
        String orderId
) {
}

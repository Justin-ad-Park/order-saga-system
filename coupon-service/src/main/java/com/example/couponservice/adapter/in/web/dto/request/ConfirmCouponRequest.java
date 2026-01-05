package com.example.couponservice.adapter.in.web.dto.request;

public record ConfirmCouponRequest(
        String couponNumber,
        String orderId
) {
}

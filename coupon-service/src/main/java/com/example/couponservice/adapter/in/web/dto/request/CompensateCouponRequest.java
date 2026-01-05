package com.example.couponservice.adapter.in.web.dto.request;

public record CompensateCouponRequest(
        String couponNumber,
        String orderId
) {
}

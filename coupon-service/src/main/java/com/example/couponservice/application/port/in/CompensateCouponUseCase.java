package com.example.couponservice.application.port.in;

public interface CompensateCouponUseCase {
    void compensateCoupon(String couponNumber, String orderId);
}

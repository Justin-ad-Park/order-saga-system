package com.example.couponservice.application.port.in;

public interface ReserveCouponUseCase {
    void reserve(String couponNumber, String orderId);
}

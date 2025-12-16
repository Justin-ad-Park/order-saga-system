package com.example.couponservice.application.port.in;

public interface ConfirmCouponUseCase {
    void confirm(String couponNumber, String orderId);
}

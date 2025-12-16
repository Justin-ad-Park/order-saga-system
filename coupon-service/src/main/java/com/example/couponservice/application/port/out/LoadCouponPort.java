package com.example.couponservice.application.port.out;

import com.example.couponservice.domain.model.Coupon;

import java.util.Optional;

public interface LoadCouponPort {
    Optional<Coupon> loadCoupon(String couponNumber);
}

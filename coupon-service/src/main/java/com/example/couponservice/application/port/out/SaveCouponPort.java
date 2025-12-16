package com.example.couponservice.application.port.out;

import com.example.couponservice.domain.model.Coupon;

public interface SaveCouponPort {
    Coupon save(Coupon coupon);
}

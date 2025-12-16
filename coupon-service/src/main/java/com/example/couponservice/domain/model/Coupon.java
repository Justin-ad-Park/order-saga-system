package com.example.couponservice.domain.model;

import com.example.couponservice.domain.model.status.CouponStatus;

import java.time.LocalDateTime;

public class Coupon {

    private final String couponNumber;
    private final CouponStatus status;
    private final LocalDateTime issuedAt;
    private final LocalDateTime expiredAt;

    public Coupon(String couponNumber,
                  CouponStatus status,
                  LocalDateTime issuedAt,
                  LocalDateTime expiredAt) {
        this.couponNumber = couponNumber;
        this.status = status;
        this.issuedAt = issuedAt;
        this.expiredAt = expiredAt;
    }

    public String couponNumber() { return couponNumber; }
    public CouponStatus status() { return status; }
    public LocalDateTime issuedAt() { return issuedAt; }
    public LocalDateTime expiredAt() { return expiredAt; }

    public boolean isAvailable() {
        return status == CouponStatus.AVAILABLE;
    }
}

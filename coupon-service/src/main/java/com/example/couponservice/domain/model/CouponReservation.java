package com.example.couponservice.domain.model;

import com.example.couponservice.domain.model.status.ReservationStatus;

public class CouponReservation {

    private final String orderId;
    private final String couponNumber;
    private final ReservationStatus status;

    public CouponReservation(String orderId, String couponNumber, ReservationStatus status) {
        this.orderId = orderId;
        this.couponNumber = couponNumber;
        this.status = status;
    }

    public String orderId() { return orderId; }
    public String couponNumber() { return couponNumber; }
    public ReservationStatus status() { return status; }
}

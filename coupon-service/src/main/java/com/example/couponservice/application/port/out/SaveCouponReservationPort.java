package com.example.couponservice.application.port.out;

import com.example.couponservice.domain.model.CouponReservation;

public interface SaveCouponReservationPort {
    CouponReservation saveReservation(CouponReservation reservation);
}

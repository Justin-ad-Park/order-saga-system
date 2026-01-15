package com.example.couponservice.application.port.out;

import com.example.couponservice.domain.model.CouponReservation;
import java.util.Optional;

public interface LoadCouponReservationPort {
    Optional<CouponReservation> loadReservation(String orderId);
}

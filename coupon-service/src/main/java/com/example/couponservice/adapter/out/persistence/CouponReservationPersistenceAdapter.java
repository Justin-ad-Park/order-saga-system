package com.example.couponservice.adapter.out.persistence;

import com.example.couponservice.adapter.out.persistence.jpa.CouponReservationJpaEntity;
import com.example.couponservice.adapter.out.persistence.jpa.CouponReservationJpaRepository;
import com.example.couponservice.application.port.out.LoadCouponReservationPort;
import com.example.couponservice.application.port.out.SaveCouponReservationPort;
import com.example.couponservice.domain.model.CouponReservation;
import com.example.couponservice.domain.model.status.ReservationStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class CouponReservationPersistenceAdapter implements LoadCouponReservationPort, SaveCouponReservationPort {

    private final CouponReservationJpaRepository couponReservationJpaRepository;

    public CouponReservationPersistenceAdapter(CouponReservationJpaRepository couponReservationJpaRepository) {
        this.couponReservationJpaRepository = couponReservationJpaRepository;
    }

    @Override
    public Optional<CouponReservation> loadReservation(String orderId) {
        return couponReservationJpaRepository.findById(orderId)
                .map(entity -> new CouponReservation(
                        entity.getOrderId(),
                        entity.getCouponNumber(),
                        entity.getStatus()
                ));
    }

    @Override
    public CouponReservation saveReservation(CouponReservation reservation) {
        LocalDateTime now = LocalDateTime.now();
        CouponReservationJpaEntity entity = couponReservationJpaRepository.findById(reservation.orderId())
                .map(existing -> new CouponReservationJpaEntity(
                        existing.getOrderId(),
                        reservation.couponNumber(),
                        reservation.status(),
                        existing.getCreatedAt(),
                        now
                ))
                .orElseGet(() -> new CouponReservationJpaEntity(
                        reservation.orderId(),
                        reservation.couponNumber(),
                        reservation.status(),
                        now,
                        now
                ));

        CouponReservationJpaEntity saved = couponReservationJpaRepository.save(entity);
        return new CouponReservation(
                saved.getOrderId(),
                saved.getCouponNumber(),
                saved.getStatus()
        );
    }
}

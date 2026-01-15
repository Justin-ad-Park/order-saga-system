package com.example.couponservice.adapter.out.persistence.jpa;

import com.example.couponservice.domain.model.status.ReservationStatus;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "coupon_reservation")
public class CouponReservationJpaEntity {

    @Id
    @Column(name = "order_id")
    private String orderId;

    @Column(name = "coupon_number", nullable = false)
    private String couponNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected CouponReservationJpaEntity() {}

    public CouponReservationJpaEntity(
            String orderId,
            String couponNumber,
            ReservationStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.orderId = orderId;
        this.couponNumber = couponNumber;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

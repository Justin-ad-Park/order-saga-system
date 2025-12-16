package com.example.couponservice.adapter.out.persistence.jpa;

import com.example.couponservice.domain.model.status.CouponStatus;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "coupon")
public class CouponJpaEntity {

    @Id
    @Column(name = "coupon_number")
    private String couponNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CouponStatus status;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    protected CouponJpaEntity() {}

    public CouponJpaEntity(String couponNumber,
                           CouponStatus status,
                           LocalDateTime issuedAt,
                           LocalDateTime expiredAt) {
        this.couponNumber = couponNumber;
        this.status = status;
        this.issuedAt = issuedAt;
        this.expiredAt = expiredAt;
    }

    // getter 생략 or Lombok @Getter 사용 가능
}

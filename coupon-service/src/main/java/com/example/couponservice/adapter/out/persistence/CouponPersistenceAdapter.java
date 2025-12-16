package com.example.couponservice.adapter.out.persistence;

import com.example.couponservice.adapter.out.persistence.jpa.CouponJpaEntity;
import com.example.couponservice.adapter.out.persistence.jpa.CouponJpaRepository;
import com.example.couponservice.application.port.out.LoadCouponPort;
import com.example.couponservice.application.port.out.SaveCouponPort;
import com.example.couponservice.domain.model.Coupon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CouponPersistenceAdapter implements LoadCouponPort, SaveCouponPort {

    private final CouponJpaRepository couponJpaRepository;

    @Override
    public Optional<Coupon> loadCoupon(String couponNumber) {
        return couponJpaRepository.findById(couponNumber)
                .map(entity -> new Coupon(
                        entity.getCouponNumber(),
                        entity.getStatus(),
                        entity.getIssuedAt(),
                        entity.getExpiredAt()
                ));
    }

    @Override
    public Coupon save(Coupon coupon) {
        CouponJpaEntity entity = new CouponJpaEntity(
                coupon.couponNumber(),
                coupon.status(),
                coupon.issuedAt(),
                coupon.expiredAt()
        );
        CouponJpaEntity saved = couponJpaRepository.save(entity);

        return new Coupon(
                saved.getCouponNumber(),
                saved.getStatus(),
                saved.getIssuedAt(),
                saved.getExpiredAt()
        );
    }
}

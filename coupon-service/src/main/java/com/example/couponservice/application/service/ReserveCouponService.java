package com.example.couponservice.application.service;

import com.example.couponservice.application.port.in.ReserveCouponUseCase;
import com.example.couponservice.application.port.out.LoadCouponPort;
import com.example.couponservice.application.port.out.SaveCouponPort;
import com.example.couponservice.domain.model.Coupon;
import com.example.couponservice.domain.model.status.CouponStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class ReserveCouponService implements ReserveCouponUseCase {

    private final LoadCouponPort loadCouponPort;
    private final SaveCouponPort saveCouponPort;

    @Override
    public void reserve(String couponNumber, String orderId) {
        Coupon coupon = loadCouponPort.loadCoupon(couponNumber)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다: " + couponNumber));

        if (!coupon.isAvailable()) {
            throw new IllegalStateException("예약 불가능한 쿠폰입니다: " + couponNumber);
        }

        // 지금은 간단히 status만 RESERVED로 변경한 새 인스턴스를 만든다고 가정
        Coupon reserved = new Coupon(
                coupon.couponNumber(),
                CouponStatus.RESERVED,
                coupon.issuedAt(),
                coupon.expiredAt()
        );

        saveCouponPort.save(reserved);
    }
}

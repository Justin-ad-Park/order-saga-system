package com.example.couponservice.application.service;

import com.example.couponservice.application.port.in.CompensateCouponUseCase;
import com.example.couponservice.application.port.in.ConfirmCouponUseCase;
import com.example.couponservice.application.port.in.ReserveCouponUseCase;
import com.example.couponservice.application.port.out.LoadCouponPort;
import com.example.couponservice.application.port.out.SaveCouponPort;
import com.example.couponservice.domain.model.Coupon;
import com.example.couponservice.domain.model.status.CouponStatus;
import jakarta.transaction.Transactional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class ReserveCouponService implements ReserveCouponUseCase, ConfirmCouponUseCase, CompensateCouponUseCase {

    private final LoadCouponPort loadCouponPort;
    private final SaveCouponPort saveCouponPort;
    @Value("${circuit-test.coupon.delay-prefix:}")
    private String delayPrefix;
    @Value("${circuit-test.coupon.delay-ms:0}")
    private long delayMs;

    @Override
    public void reserve(String couponNumber, String orderId) {
        maybeDelay(couponNumber);
        updateStatus(couponNumber, CouponStatus.RESERVED, this::validateReservable);
    }

    @Override
    public void confirm(String couponNumber, String orderId) {
        Coupon coupon = loadCouponPort.loadCoupon(couponNumber)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다: " + couponNumber));
        if (coupon.status() == CouponStatus.USED) {
            return;
        }
        validateConfirmable(coupon);

        Coupon updated = new Coupon(
                coupon.couponNumber(),
                CouponStatus.USED,
                coupon.issuedAt(),
                coupon.expiredAt()
        );
        saveCouponPort.save(updated);
    }

    @Override
    public void compensateCoupon(String couponNumber, String orderId) {
        Coupon coupon = loadCouponPort.loadCoupon(couponNumber)
                .orElse(null);
        if (coupon == null) {
            return;
        }
        if (coupon.status() == CouponStatus.USED) {
            throw new IllegalStateException("보상 불가능한 쿠폰입니다: " + coupon.couponNumber());
        }
        if (coupon.status() != CouponStatus.RESERVED) {
            return;
        }

        Coupon updated = new Coupon(
                coupon.couponNumber(),
                CouponStatus.AVAILABLE,
                coupon.issuedAt(),
                coupon.expiredAt()
        );
        saveCouponPort.save(updated);
    }

    private void updateStatus(
            String couponNumber,
            CouponStatus targetStatus,
            Consumer<Coupon> validator
    ) {
        Coupon coupon = loadCouponPort.loadCoupon(couponNumber)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다: " + couponNumber));

        validator.accept(coupon);

        Coupon updated = new Coupon(
                coupon.couponNumber(),
                targetStatus,
                coupon.issuedAt(),
                coupon.expiredAt()
        );

        saveCouponPort.save(updated);
    }

    private void validateReservable(Coupon coupon) {
        if (!coupon.isAvailable()) {
            throw new IllegalStateException("예약 불가능한 쿠폰입니다: " + coupon.couponNumber());
        }
    }

    private void validateConfirmable(Coupon coupon) {
        if (coupon.status() != CouponStatus.RESERVED) {
            throw new IllegalStateException("확정 불가능한 쿠폰입니다: " + coupon.couponNumber());
        }
    }

    private void maybeDelay(String couponNumber) {
        if (delayMs <= 0 || delayPrefix == null || delayPrefix.isBlank()) {
            return;
        }
        if (!couponNumber.startsWith(delayPrefix)) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Delay interrupted", ex);
        }
    }

}

package com.example.couponservice.application.service;

import com.example.couponservice.application.port.in.ReserveCouponUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Primary
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class ReserveCouponDelayDecorator implements ReserveCouponUseCase {

    private final ReserveCouponService delegate;

    @Value("${circuit-test.coupon.delay-enabled:false}")
    private boolean delayEnabled;
    @Value("${circuit-test.coupon.delay-prefix:}")
    private String delayPrefix;
    @Value("${circuit-test.coupon.delay-ms:0}")
    private long delayMs;

    @Override
    public void reserve(String couponNumber, String orderId) {
        maybeDelay(couponNumber);
        delegate.reserve(couponNumber, orderId);
    }

    private void maybeDelay(String couponNumber) {
        if (!delayEnabled) {
            return;
        }
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

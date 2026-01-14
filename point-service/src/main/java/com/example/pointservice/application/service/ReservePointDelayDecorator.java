package com.example.pointservice.application.service;

import com.example.pointservice.application.port.in.ReservePointUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Primary
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class ReservePointDelayDecorator implements ReservePointUseCase {

    private final ReservePointService delegate;

    @Value("${circuit-test.point.delay-enabled:false}")
    private boolean delayEnabled;
    @Value("${circuit-test.point.delay-prefix:}")
    private String delayPrefix;
    @Value("${circuit-test.point.delay-ms:0}")
    private long delayMs;

    @Override
    public void reserve(String pointNumber, String orderId) {
        maybeDelay(pointNumber);
        delegate.reserve(pointNumber, orderId);
    }

    private void maybeDelay(String pointNumber) {
        if (!delayEnabled) {
            return;
        }
        if (delayMs <= 0 || delayPrefix == null || delayPrefix.isBlank()) {
            return;
        }
        if (!pointNumber.startsWith(delayPrefix)) {
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

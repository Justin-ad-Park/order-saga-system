package com.example.pointservice.application.service;

import com.example.pointservice.application.port.in.CompensatePointUseCase;
import com.example.pointservice.application.port.in.ConfirmPointUseCase;
import com.example.pointservice.application.port.in.ReservePointUseCase;
import com.example.pointservice.application.port.out.LoadPointPort;
import com.example.pointservice.application.port.out.SavePointPort;
import com.example.pointservice.domain.model.Point;
import com.example.pointservice.domain.model.status.PointStatus;
import jakarta.transaction.Transactional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservePointService implements ReservePointUseCase, ConfirmPointUseCase, CompensatePointUseCase {

    private final LoadPointPort loadPointPort;
    private final SavePointPort savePointPort;
    @Value("${circuit-test.point.delay-prefix:}")
    private String delayPrefix;
    @Value("${circuit-test.point.delay-ms:0}")
    private long delayMs;

    @Override
    public void reserve(String pointNumber, String orderId) {
        maybeDelay(pointNumber);
        updateStatus(pointNumber, PointStatus.RESERVED, this::validateReservable);
    }

    @Override
    public void confirm(String pointNumber, String orderId) {
        Point point = loadPointPort.loadPoint(pointNumber)
                .orElseThrow(() -> new IllegalArgumentException("포인트를 찾을 수 없습니다: " + pointNumber));
        if (point.status() == PointStatus.USED) {
            return;
        }
        validateConfirmable(point);

        Point updated = new Point(
                point.pointNumber(),
                PointStatus.USED,
                point.issuedAt(),
                point.expiredAt()
        );
        savePointPort.save(updated);
    }

    @Override
    public void compensatePoint(String pointNumber, String orderId) {
        Point point = loadPointPort.loadPoint(pointNumber)
                .orElse(null);
        if (point == null) {
            return;
        }
        if (point.status() == PointStatus.USED) {
            throw new IllegalStateException("보상 불가능한 포인트입니다: " + point.pointNumber());
        }
        if (point.status() != PointStatus.RESERVED) {
            return;
        }

        Point updated = new Point(
                point.pointNumber(),
                PointStatus.AVAILABLE,
                point.issuedAt(),
                point.expiredAt()
        );
        savePointPort.save(updated);
    }

    private void updateStatus(
            String pointNumber,
            PointStatus targetStatus,
            Consumer<Point> validator
    ) {
        Point point = loadPointPort.loadPoint(pointNumber)
                .orElseThrow(() -> new IllegalArgumentException("포인트를 찾을 수 없습니다: " + pointNumber));

        validator.accept(point);

        Point updated = new Point(
                point.pointNumber(),
                targetStatus,
                point.issuedAt(),
                point.expiredAt()
        );

        savePointPort.save(updated);
    }

    private void validateReservable(Point point) {
        if (!point.isAvailable()) {
            throw new IllegalStateException("예약 불가능한 포인트입니다: " + point.pointNumber());
        }
    }

    private void validateConfirmable(Point point) {
        if (point.status() != PointStatus.RESERVED) {
            throw new IllegalStateException("확정 불가능한 포인트입니다: " + point.pointNumber());
        }
    }

    private void maybeDelay(String pointNumber) {
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

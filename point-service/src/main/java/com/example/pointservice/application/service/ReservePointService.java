package com.example.pointservice.application.service;

import com.example.pointservice.application.port.in.CompensatePointUseCase;
import com.example.pointservice.application.port.in.ConfirmPointUseCase;
import com.example.pointservice.application.port.in.ReservePointUseCase;
import com.example.pointservice.application.port.out.LoadPointPort;
import com.example.pointservice.application.port.out.LoadPointReservationPort;
import com.example.pointservice.application.port.out.SavePointPort;
import com.example.pointservice.application.port.out.SavePointReservationPort;
import com.example.pointservice.domain.model.Point;
import com.example.pointservice.domain.model.PointReservation;
import com.example.pointservice.domain.model.status.PointStatus;
import com.example.pointservice.domain.model.status.ReservationStatus;
import jakarta.transaction.Transactional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservePointService implements ReservePointUseCase, ConfirmPointUseCase, CompensatePointUseCase {

    private final LoadPointPort loadPointPort;
    private final SavePointPort savePointPort;
    private final LoadPointReservationPort loadPointReservationPort;
    private final SavePointReservationPort savePointReservationPort;

    @Override
    public void reserve(String pointNumber, String orderId) {
        // 이미 보상 처리된 주문이면 예약 진행하지 않음
        if (isReservationCancelled(orderId)) {
            return;
        }

        //이미 예약된 주문이면 예약 진행하지 않음
        verifyReservationNotAlreadyReserved(orderId);


        updateStatus(pointNumber, PointStatus.RESERVED, this::validateReservable);
        savePointReservationPort.saveReservation(new PointReservation(
                orderId,
                pointNumber,
                ReservationStatus.RESERVED
        ));
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
            saveReservationCancelled(orderId, pointNumber);
            return;
        }
        if (point.status() == PointStatus.USED) {
            throw new IllegalStateException("보상 불가능한 포인트입니다: " + point.pointNumber());
        }

        saveReservationCancelled(orderId, pointNumber);
        if (point.status() != PointStatus.RESERVED) {   // RESERVED 일 때만 보상처리
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

    private boolean isReservationCancelled(String orderId) {
        return loadPointReservationPort.loadReservation(orderId)
                .map(reservation -> reservation.status() == ReservationStatus.CANCELLED)
                .orElse(false);
    }

    private void verifyReservationNotAlreadyReserved(String orderId) {
        loadPointReservationPort.loadReservation(orderId)
                .filter(reservation -> reservation.status() == ReservationStatus.RESERVED)
                .ifPresent(reservation -> {
                    throw new IllegalStateException("이미 예약된 주문입니다: " + reservation.orderId());
                });
    }

    private void saveReservationCancelled(String orderId, String pointNumber) {
        savePointReservationPort.saveReservation(new PointReservation(
                orderId,
                pointNumber,
                ReservationStatus.CANCELLED
        ));
    }
}

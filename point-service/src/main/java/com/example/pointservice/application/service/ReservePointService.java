package com.example.pointservice.application.service;

import com.example.pointservice.application.port.in.ReservePointUseCase;
import com.example.pointservice.application.port.out.LoadPointPort;
import com.example.pointservice.application.port.out.SavePointPort;
import com.example.pointservice.domain.model.Point;
import com.example.pointservice.domain.model.status.PointStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservePointService implements ReservePointUseCase {

    private final LoadPointPort loadPointPort;
    private final SavePointPort savePointPort;

    @Override
    public void reserve(String pointNumber, String orderId) {
        Point point = loadPointPort.loadPoint(pointNumber)
                .orElseThrow(() -> new IllegalArgumentException("포인트를 찾을 수 없습니다: " + pointNumber));

        if (!point.isAvailable()) {
            throw new IllegalStateException("예약 불가능한 포인트입니다: " + pointNumber);
        }

        // 지금은 간단히 status만 RESERVED로 변경한 새 인스턴스를 만든다고 가정
        Point reserved = new Point(
                point.pointNumber(),
                PointStatus.RESERVED,
                point.issuedAt(),
                point.expiredAt()
        );

        savePointPort.save(reserved);
    }
}

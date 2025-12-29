package com.example.pointservice.adapter.out.persistence;

import com.example.pointservice.adapter.out.persistence.jpa.PointJpaEntity;
import com.example.pointservice.adapter.out.persistence.jpa.PointJpaRepository;
import com.example.pointservice.application.port.out.LoadPointPort;
import com.example.pointservice.application.port.out.SavePointPort;
import com.example.pointservice.domain.model.Point;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PointPersistenceAdapter implements LoadPointPort, SavePointPort {

    private final PointJpaRepository pointJpaRepository;

    @Override
    public Optional<Point> loadPoint(String pointNumber) {
        return pointJpaRepository.findById(pointNumber)
                .map(entity -> new Point(
                        entity.getPointNumber(),
                        entity.getStatus(),
                        entity.getIssuedAt(),
                        entity.getExpiredAt()
                ));
    }

    @Override
    public Point save(Point point) {
        PointJpaEntity entity = new PointJpaEntity(
                point.pointNumber(),
                point.status(),
                point.issuedAt(),
                point.expiredAt()
        );
        PointJpaEntity saved = pointJpaRepository.save(entity);

        return new Point(
                saved.getPointNumber(),
                saved.getStatus(),
                saved.getIssuedAt(),
                saved.getExpiredAt()
        );
    }
}

package com.example.pointservice.adapter.out.persistence;

import com.example.pointservice.adapter.out.persistence.jpa.PointReservationJpaEntity;
import com.example.pointservice.adapter.out.persistence.jpa.PointReservationJpaRepository;
import com.example.pointservice.application.port.out.LoadPointReservationPort;
import com.example.pointservice.application.port.out.SavePointReservationPort;
import com.example.pointservice.domain.model.PointReservation;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class PointReservationPersistenceAdapter implements LoadPointReservationPort, SavePointReservationPort {

    private final PointReservationJpaRepository pointReservationJpaRepository;

    public PointReservationPersistenceAdapter(PointReservationJpaRepository pointReservationJpaRepository) {
        this.pointReservationJpaRepository = pointReservationJpaRepository;
    }

    @Override
    public Optional<PointReservation> loadReservation(String orderId) {
        return pointReservationJpaRepository.findById(orderId)
                .map(entity -> new PointReservation(
                        entity.getOrderId(),
                        entity.getPointNumber(),
                        entity.getStatus()
                ));
    }

    @Override
    public PointReservation saveReservation(PointReservation reservation) {
        LocalDateTime now = LocalDateTime.now();
        PointReservationJpaEntity entity = pointReservationJpaRepository.findById(reservation.orderId())
                .map(existing -> new PointReservationJpaEntity(
                        existing.getOrderId(),
                        reservation.pointNumber(),
                        reservation.status(),
                        existing.getCreatedAt(),
                        now
                ))
                .orElseGet(() -> new PointReservationJpaEntity(
                        reservation.orderId(),
                        reservation.pointNumber(),
                        reservation.status(),
                        now,
                        now
                ));

        PointReservationJpaEntity saved = pointReservationJpaRepository.save(entity);
        return new PointReservation(
                saved.getOrderId(),
                saved.getPointNumber(),
                saved.getStatus()
        );
    }
}

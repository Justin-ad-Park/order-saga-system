package com.example.pointservice.adapter.out.persistence.jpa;

import com.example.pointservice.domain.model.status.ReservationStatus;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "point_reservation")
public class PointReservationJpaEntity {

    @Id
    @Column(name = "order_id")
    private String orderId;

    @Column(name = "point_number", nullable = false)
    private String pointNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected PointReservationJpaEntity() {}

    public PointReservationJpaEntity(
            String orderId,
            String pointNumber,
            ReservationStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.orderId = orderId;
        this.pointNumber = pointNumber;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

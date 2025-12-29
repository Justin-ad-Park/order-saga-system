package com.example.pointservice.adapter.out.persistence.jpa;

import com.example.pointservice.domain.model.status.PointStatus;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "point")
public class PointJpaEntity {

    @Id
    @Column(name = "point_number")
    private String pointNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PointStatus status;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    protected PointJpaEntity() {}

    public PointJpaEntity(String pointNumber,
                           PointStatus status,
                           LocalDateTime issuedAt,
                           LocalDateTime expiredAt) {
        this.pointNumber = pointNumber;
        this.status = status;
        this.issuedAt = issuedAt;
        this.expiredAt = expiredAt;
    }

    // getter 생략 or Lombok @Getter 사용 가능
}

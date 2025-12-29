package com.example.pointservice.domain.model;

import com.example.pointservice.domain.model.status.PointStatus;

import java.time.LocalDateTime;

public class Point {

    private final String pointNumber;
    private final PointStatus status;
    private final LocalDateTime issuedAt;
    private final LocalDateTime expiredAt;

    public Point(String pointNumber,
                  PointStatus status,
                  LocalDateTime issuedAt,
                  LocalDateTime expiredAt) {
        this.pointNumber = pointNumber;
        this.status = status;
        this.issuedAt = issuedAt;
        this.expiredAt = expiredAt;
    }

    public String pointNumber() { return pointNumber; }
    public PointStatus status() { return status; }
    public LocalDateTime issuedAt() { return issuedAt; }
    public LocalDateTime expiredAt() { return expiredAt; }

    public boolean isAvailable() {
        return status == PointStatus.AVAILABLE;
    }
}

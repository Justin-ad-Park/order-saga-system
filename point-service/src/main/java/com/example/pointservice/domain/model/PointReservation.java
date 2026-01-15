package com.example.pointservice.domain.model;

import com.example.pointservice.domain.model.status.ReservationStatus;

public class PointReservation {

    private final String orderId;
    private final String pointNumber;
    private final ReservationStatus status;

    public PointReservation(String orderId, String pointNumber, ReservationStatus status) {
        this.orderId = orderId;
        this.pointNumber = pointNumber;
        this.status = status;
    }

    public String orderId() { return orderId; }
    public String pointNumber() { return pointNumber; }
    public ReservationStatus status() { return status; }
}

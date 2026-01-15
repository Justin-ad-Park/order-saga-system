package com.example.pointservice.application.port.out;

import com.example.pointservice.domain.model.PointReservation;

public interface SavePointReservationPort {
    PointReservation saveReservation(PointReservation reservation);
}

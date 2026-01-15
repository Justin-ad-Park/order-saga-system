package com.example.pointservice.application.port.out;

import com.example.pointservice.domain.model.PointReservation;
import java.util.Optional;

public interface LoadPointReservationPort {
    Optional<PointReservation> loadReservation(String orderId);
}

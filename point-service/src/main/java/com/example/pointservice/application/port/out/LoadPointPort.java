package com.example.pointservice.application.port.out;

import com.example.pointservice.domain.model.Point;

import java.util.Optional;

public interface LoadPointPort {
    Optional<Point> loadPoint(String pointNumber);
}

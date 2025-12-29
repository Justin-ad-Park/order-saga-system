package com.example.pointservice.application.port.out;

import com.example.pointservice.domain.model.Point;

public interface SavePointPort {
    Point save(Point point);
}

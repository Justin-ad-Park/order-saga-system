package com.example.pointservice.application.port.in;

public interface CompensatePointUseCase {
    void compensatePoint(String pointNumber, String orderId);
}

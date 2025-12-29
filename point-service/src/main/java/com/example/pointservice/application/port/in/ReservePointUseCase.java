package com.example.pointservice.application.port.in;

public interface ReservePointUseCase {
    void reserve(String pointNumber, String orderId);
}

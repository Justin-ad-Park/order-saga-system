package com.example.pointservice.application.port.in;

public interface ConfirmPointUseCase {
    void confirm(String pointNumber, String orderId);
}

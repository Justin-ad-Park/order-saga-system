package com.example.ordersagaconsumer.application.port.in;

public interface ProcessOrderSagaEventUseCase {
    void process(String orderId, String status);
}

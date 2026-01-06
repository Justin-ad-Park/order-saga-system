package com.example.ordersagaconsumer.application.port.out;

public interface PointServicePort {
    boolean confirm(String pointNumber, String orderId);
    boolean compensate(String pointNumber, String orderId);
}

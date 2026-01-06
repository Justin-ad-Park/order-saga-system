package com.example.ordersagaconsumer.application.port.out;

public interface CouponServicePort {
    boolean confirm(String couponNumber, String orderId);
    boolean compensate(String couponNumber, String orderId);
}

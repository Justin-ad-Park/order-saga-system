package com.example.ordersagaconsumer.domain.model;

public record OrderSagaInfo(
        String orderId,
        String couponNumber,
        String pointNumber
) {
}

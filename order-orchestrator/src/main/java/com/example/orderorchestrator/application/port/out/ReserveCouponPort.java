package com.example.orderorchestrator.application.port.out;

import reactor.core.publisher.Mono;

public interface ReserveCouponPort {
    Mono<Void> reserveCoupon(String couponNumber, String orderId);
}

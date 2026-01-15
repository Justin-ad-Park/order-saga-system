package com.example.orderorchestrator.application.port.out;

import reactor.core.publisher.Mono;

public interface ReservePointPort {
    Mono<Void> reservePoint(String pointNumber, String orderId);
}

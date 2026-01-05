package com.example.orderorchestrator.application.port.out;

import com.example.orderorchestrator.domain.event.OrderSagaEvent;

public interface OrderSagaEventPublisher {
    void publish(OrderSagaEvent event);
}

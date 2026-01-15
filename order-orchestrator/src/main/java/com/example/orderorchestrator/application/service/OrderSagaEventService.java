package com.example.orderorchestrator.application.service;

import com.example.common.status.OrderSagaStatus;
import com.example.orderorchestrator.application.port.out.OrderSagaEventPublisher;
import com.example.orderorchestrator.domain.event.OrderSagaEvent;
import com.example.orderorchestrator.domain.event.OrderSagaEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderSagaEventService {

    private final OrderSagaEventPublisher orderSagaEventPublisher;

    public void publish(String orderId, String sagaId, OrderSagaStatus status, OrderSagaEventType type) {
        OrderSagaEvent event = new OrderSagaEvent(orderId, sagaId, type, status);
        orderSagaEventPublisher.publish(event);
    }
}

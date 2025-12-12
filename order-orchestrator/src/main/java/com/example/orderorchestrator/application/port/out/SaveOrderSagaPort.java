package com.example.orderorchestrator.application.port.out;

import com.example.orderorchestrator.domain.model.OrderSaga;

public interface SaveOrderSagaPort {
    OrderSaga save(OrderSaga orderSaga);
}

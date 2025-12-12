package com.example.orderorchestrator.application.port.in;

import com.example.orderorchestrator.application.port.in.command.CreateOrderCommand;
import com.example.orderorchestrator.application.port.in.result.CreateOrderResult;

public interface CreateOrderUseCase {

    CreateOrderResult createOrder(CreateOrderCommand command);
}
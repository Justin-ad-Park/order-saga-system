package com.example.orderorchestrator.application.port.in.command;

import java.util.List;

public record CreateOrderCommand(
        String couponNumber,
        String pointNumber,
        String paymentNumber,
        long paymentAmount,
        List<OrderItemCommand> orderItems
) {

    public record OrderItemCommand(
            String itemNumber,
            int quantity
    ) {}
}

package com.example.orderorchestrator.domain.model;

public record OrderItem(
        String itemNumber,
        int quantity
) {
}
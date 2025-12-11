package com.example.orderorchestrator.application.port.in.command;

public record DepositCommand(
        String accountNumber,
        long amount
) {
}

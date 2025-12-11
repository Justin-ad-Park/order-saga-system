package com.example.orderorchestrator.application.port.in.command;

public record WithdrawCommand(
        String accountNumber,
        long amount
) {
}

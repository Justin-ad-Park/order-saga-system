package com.example.orderorchestrator.domain.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String accountNumber) {
        super("Account not found: " + accountNumber);
    }
}
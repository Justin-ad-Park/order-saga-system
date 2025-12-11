package com.example.orderorchestrator.application.port.in;

import com.example.orderorchestrator.domain.model.Account;

public interface CreateAccountUseCase {
    Account createAccount(String accountNumber, String name, long initialBalance);
}

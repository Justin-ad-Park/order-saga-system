package com.example.orderorchestrator.application.port.out;

import com.example.orderorchestrator.domain.model.Account;

public interface LoadAccountPort {
    Account load(String accountNumber);
}

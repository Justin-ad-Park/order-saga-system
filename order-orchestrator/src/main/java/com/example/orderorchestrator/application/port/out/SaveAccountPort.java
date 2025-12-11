package com.example.orderorchestrator.application.port.out;

import com.example.orderorchestrator.domain.model.Account;

public interface SaveAccountPort {
    void save(Account account);
}

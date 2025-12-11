package com.example.orderorchestrator.application.port.in;

import com.example.orderorchestrator.domain.model.Account;

public interface GetAccountQuery {
    Account getAccount(String accountNumber);
}

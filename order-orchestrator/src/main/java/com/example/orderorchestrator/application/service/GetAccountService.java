package com.example.orderorchestrator.application.service;

import com.example.orderorchestrator.application.port.in.GetAccountQuery;
import com.example.orderorchestrator.application.port.out.LoadAccountPort;
import com.example.orderorchestrator.domain.model.Account;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class GetAccountService implements GetAccountQuery {

    private final LoadAccountPort loadAccountPort;

    GetAccountService(LoadAccountPort loadAccountPort) {
        this.loadAccountPort = loadAccountPort;
    }


    @Override
    public Account getAccount(String accountNumber) {
        return loadAccountPort.load(accountNumber);
    }
}

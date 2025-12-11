package com.example.orderorchestrator.application.port.in;

import com.example.orderorchestrator.application.port.in.command.DepositCommand;
import com.example.orderorchestrator.domain.model.Account;

public interface DepositUseCase {
    Account deposit(DepositCommand depositCommand);
}

package com.example.orderorchestrator.application.port.in;

import com.example.orderorchestrator.application.port.in.command.WithdrawCommand;
import com.example.orderorchestrator.domain.model.Account;

public interface WithdrawUseCase {
    Account withdraw(WithdrawCommand withdrawCommand);
}

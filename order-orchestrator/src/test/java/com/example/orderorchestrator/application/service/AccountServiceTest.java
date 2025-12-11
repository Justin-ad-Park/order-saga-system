package com.example.orderorchestrator.application.service;

import com.example.orderorchestrator.application.port.in.GetAccountQuery;
import com.example.orderorchestrator.application.port.in.command.DepositCommand;
import com.example.orderorchestrator.application.port.in.command.WithdrawCommand;
import com.example.orderorchestrator.domain.exception.AccountNotFoundException;
import com.example.orderorchestrator.application.port.in.CreateAccountUseCase;
import com.example.orderorchestrator.application.port.in.DepositUseCase;
import com.example.orderorchestrator.application.port.in.WithdrawUseCase;
import com.example.orderorchestrator.domain.model.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("file")
class AccountServiceTest {

    @Autowired CreateAccountUseCase createAccountUseCase;
    @Autowired DepositUseCase depositUseCase;
    @Autowired WithdrawUseCase withdrawUseCase;
    @Autowired GetAccountQuery getAccountQuery;


    @Test
    void createAndDepositAndWithdraw() {
        Account account = createAccountUseCase.createAccount("123", "Alice", 1000L);
        assertEquals(1000L, account.getBalance());

//        account = getAccountQuery.getAccount("123");
//        assertEquals(1000L, account.getBalance());

        account = depositUseCase.deposit(new DepositCommand("123", 500));
        assertEquals(1500L, account.getBalance());

        account = withdrawUseCase.withdraw(new WithdrawCommand("123", 300));
        assertEquals(1200L, account.getBalance());

        account = getAccountQuery.getAccount("123");
        assertEquals(1200L, account.getBalance());
    }

    @Test
    void getAccount_shouldThrow_whenAccountDoesNotExist() {
        // 없는 계좌를 조회할 때 예외 발생을 검증
        assertThrows(AccountNotFoundException.class, () -> {
            getAccountQuery.getAccount("noAccount");
        });
    }
}

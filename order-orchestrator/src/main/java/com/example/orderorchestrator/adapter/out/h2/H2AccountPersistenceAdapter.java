package com.example.orderorchestrator.adapter.out.h2;

import com.example.orderorchestrator.adapter.out.h2.mybatis.AccountRow;
import com.example.orderorchestrator.application.port.out.LoadAccountPort;
import com.example.orderorchestrator.application.port.out.SaveAccountPort;
import com.example.orderorchestrator.domain.exception.AccountNotFoundException;
import com.example.orderorchestrator.domain.model.Account;
import com.example.orderorchestrator.adapter.out.h2.mapper.AccountMapper;

/**
 * 쿼리는 모두 MyBatis XML로 분리.
 * 어댑터는 Port 구현과 도메인 변환만 담당.
 */
class H2AccountPersistenceAdapter implements LoadAccountPort, SaveAccountPort {

    private final AccountMapper mapper;

    H2AccountPersistenceAdapter(AccountMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Account load(String accountNumber) {
        var row = mapper.findByAccountNumber(accountNumber);

        if (row == null) {
            throw new AccountNotFoundException("Account not found: " + accountNumber);
        }
        return Account.of(row.getAccountNumber(), row.getName(), row.getBalance());
    }

    @Override
    public void save(Account account) {
        var row = AccountRow.of(account);
        mapper.upsert(row);
    }
}

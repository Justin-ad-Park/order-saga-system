package com.example.orderorchestrator.adapter.out.h2;

import com.example.orderorchestrator.application.port.out.LoadAccountPort;
import com.example.orderorchestrator.application.port.out.SaveAccountPort;
import com.example.orderorchestrator.domain.model.Account;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("h2")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class H2AccountPersistenceAdapterTest {
    @Autowired
    LoadAccountPort loadAccountPort;

    @Autowired
    SaveAccountPort saveAccountPort;

    @Test @Order(1)
    void save() {
        Account account = Account.of("123", "Alice", 1000L);
        saveAccountPort.save(account);
    }

    @Test @Order(2)
    void load() {
        String accountId = "123";
        Account account = loadAccountPort.load(accountId);

        Assertions.assertEquals(1000L, account.getBalance());
    }

}
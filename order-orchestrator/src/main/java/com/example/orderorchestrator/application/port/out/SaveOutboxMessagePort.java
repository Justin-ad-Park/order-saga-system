package com.example.orderorchestrator.application.port.out;

import com.example.orderorchestrator.domain.outbox.OutboxMessage;

public interface SaveOutboxMessagePort {
    OutboxMessage save(OutboxMessage message);
}

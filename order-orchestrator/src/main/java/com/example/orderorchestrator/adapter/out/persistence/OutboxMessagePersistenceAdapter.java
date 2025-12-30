// src/main/java/com/example/orderorchestrator/adapter/out/persistence/OutboxMessagePersistenceAdapter.java
package com.example.orderorchestrator.adapter.out.persistence;

import com.example.orderorchestrator.application.port.out.SaveOutboxMessagePort;
import com.example.orderorchestrator.application.port.out.UpdateOutboxMessagePort;
import com.example.orderorchestrator.domain.outbox.OutboxMessage;
import com.example.orderorchestrator.domain.model.status.MSAStatus;
import com.example.orderorchestrator.adapter.out.persistence.jpa.OutboxMessageJpaRepository;
import com.example.orderorchestrator.adapter.out.persistence.jpa.entity.OutboxMessageJpaEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
@Transactional
public class OutboxMessagePersistenceAdapter implements SaveOutboxMessagePort, UpdateOutboxMessagePort {

    private final OutboxMessageJpaRepository outboxMessageJpaRepository;

    public OutboxMessagePersistenceAdapter(OutboxMessageJpaRepository outboxMessageJpaRepository) {
        this.outboxMessageJpaRepository = outboxMessageJpaRepository;
    }

    @Override
    public OutboxMessage save(OutboxMessage message) {
        OutboxMessageJpaEntity entity = new OutboxMessageJpaEntity(
                message.orderId(),
                message.payload(),
                message.couponStatus(),
                message.pointStatus(),
                message.orderStatus(),
                message.paymentStatus(),
                message.sagaStatus(),
                message.createdAt(),
                message.updatedAt()
        );

        OutboxMessageJpaEntity saved = outboxMessageJpaRepository.save(entity);

        // id는 현재 도메인 OutboxMessage에 없으니,
        // 필요하면 나중에 OutboxMessage에 id 필드를 추가하고 여기서 반영해도 됨.
        return new OutboxMessage(
                saved.getOrderId(),
                saved.getPayload(),
                saved.getCouponStatus(),
                saved.getPointStatus(),
                saved.getOrderStatus(),
                saved.getPaymentStatus(),
                saved.getSagaStatus(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    @Override
    public void updateCouponStatus(String orderId, MSAStatus status) {
        OutboxMessageJpaEntity entity = outboxMessageJpaRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Outbox message not found: " + orderId));
        entity.setCouponStatus(status);
        entity.setUpdatedAt(LocalDateTime.now());
        outboxMessageJpaRepository.save(entity);
    }

    @Override
    public void updatePointStatus(String orderId, MSAStatus status) {
        OutboxMessageJpaEntity entity = outboxMessageJpaRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Outbox message not found: " + orderId));
        entity.setPointStatus(status);
        entity.setUpdatedAt(LocalDateTime.now());
        outboxMessageJpaRepository.save(entity);
    }
}

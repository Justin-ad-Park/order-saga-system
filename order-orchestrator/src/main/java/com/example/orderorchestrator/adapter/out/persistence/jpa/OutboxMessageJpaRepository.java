// src/main/java/com/example/orderorchestrator/adapter/out/persistence/jpa/OutboxMessageJpaRepository.java
package com.example.orderorchestrator.adapter.out.persistence.jpa;

import com.example.orderorchestrator.adapter.out.persistence.jpa.entity.OutboxMessageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OutboxMessageJpaRepository extends JpaRepository<OutboxMessageJpaEntity, Long> {
    Optional<OutboxMessageJpaEntity> findByOrderId(String orderId);
}

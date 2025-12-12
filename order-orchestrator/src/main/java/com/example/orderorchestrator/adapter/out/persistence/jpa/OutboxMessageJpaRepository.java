// src/main/java/com/example/orderorchestrator/adapter/out/persistence/jpa/OutboxMessageJpaRepository.java
package com.example.orderorchestrator.adapter.out.persistence.jpa;

import com.example.orderorchestrator.adapter.out.persistence.jpa.entity.OutboxMessageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxMessageJpaRepository extends JpaRepository<OutboxMessageJpaEntity, Long> {
}

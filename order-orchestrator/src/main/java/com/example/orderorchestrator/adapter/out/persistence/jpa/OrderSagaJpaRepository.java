// src/main/java/com/example/orderorchestrator/adapter/out/persistence/jpa/OrderSagaJpaRepository.java
package com.example.orderorchestrator.adapter.out.persistence.jpa;

import com.example.orderorchestrator.adapter.out.persistence.jpa.entity.OrderSagaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderSagaJpaRepository extends JpaRepository<OrderSagaJpaEntity, Long> {
    Optional<OrderSagaJpaEntity> findByOrderId(String orderId);
}

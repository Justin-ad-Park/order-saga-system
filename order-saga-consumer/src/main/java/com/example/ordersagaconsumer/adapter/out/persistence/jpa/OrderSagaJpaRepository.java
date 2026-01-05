package com.example.ordersagaconsumer.adapter.out.persistence.jpa;

import com.example.ordersagaconsumer.adapter.out.persistence.jpa.entity.OrderSagaJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderSagaJpaRepository extends JpaRepository<OrderSagaJpaEntity, Long> {
    Optional<OrderSagaJpaEntity> findByOrderId(String orderId);
}

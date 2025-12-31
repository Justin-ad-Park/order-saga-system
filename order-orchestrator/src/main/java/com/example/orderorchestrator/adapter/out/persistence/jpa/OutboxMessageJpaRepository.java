// src/main/java/com/example/orderorchestrator/adapter/out/persistence/jpa/OutboxMessageJpaRepository.java
package com.example.orderorchestrator.adapter.out.persistence.jpa;

import com.example.orderorchestrator.adapter.out.persistence.jpa.entity.OutboxMessageJpaEntity;
import com.example.orderorchestrator.domain.model.status.MSAStatus;
import com.example.orderorchestrator.domain.model.status.OrderSagaStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OutboxMessageJpaRepository extends JpaRepository<OutboxMessageJpaEntity, Long> {
    Optional<OutboxMessageJpaEntity> findByOrderId(String orderId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update OutboxMessageJpaEntity o set o.couponStatus = :status, o.updatedAt = :updatedAt where o.orderId = :orderId")
    int updateCouponStatus(
            @Param("orderId") String orderId,
            @Param("status") MSAStatus status,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update OutboxMessageJpaEntity o set o.pointStatus = :status, o.updatedAt = :updatedAt where o.orderId = :orderId")
    int updatePointStatus(
            @Param("orderId") String orderId,
            @Param("status") MSAStatus status,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update OutboxMessageJpaEntity o set o.sagaStatus = :status, o.updatedAt = :updatedAt where o.orderId = :orderId")
    int updateSagaStatus(
            @Param("orderId") String orderId,
            @Param("status") OrderSagaStatus status,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}

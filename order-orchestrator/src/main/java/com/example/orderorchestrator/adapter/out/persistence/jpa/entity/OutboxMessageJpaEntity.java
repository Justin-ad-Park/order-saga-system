// src/main/java/com/example/orderorchestrator/adapter/out/persistence/jpa/OutboxMessageJpaEntity.java
package com.example.orderorchestrator.adapter.out.persistence.jpa.entity;

import com.example.orderorchestrator.domain.model.status.MSAStatus;
import com.example.orderorchestrator.domain.model.status.OrderSagaStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_message")
public class OutboxMessageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_status", nullable = false)
    private MSAStatus couponStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "point_status", nullable = false)
    private MSAStatus pointStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private MSAStatus orderStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private MSAStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "saga_status", nullable = false)
    private OrderSagaStatus sagaStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected OutboxMessageJpaEntity() {
    }

    public OutboxMessageJpaEntity(
            String orderId,
            String payload,
            MSAStatus couponStatus,
            MSAStatus pointStatus,
            MSAStatus orderStatus,
            MSAStatus paymentStatus,
            OrderSagaStatus sagaStatus,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.orderId = orderId;
        this.payload = payload;
        this.couponStatus = couponStatus;
        this.pointStatus = pointStatus;
        this.orderStatus = orderStatus;
        this.paymentStatus = paymentStatus;
        this.sagaStatus = sagaStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getPayload() { return payload; }
    public MSAStatus getCouponStatus() { return couponStatus; }
    public MSAStatus getPointStatus() { return pointStatus; }
    public MSAStatus getOrderStatus() { return orderStatus; }
    public MSAStatus getPaymentStatus() { return paymentStatus; }
    public OrderSagaStatus getSagaStatus() { return sagaStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

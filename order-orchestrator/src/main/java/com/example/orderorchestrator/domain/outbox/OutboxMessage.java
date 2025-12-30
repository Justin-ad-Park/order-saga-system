package com.example.orderorchestrator.domain.outbox;

import java.time.Instant;

import com.example.orderorchestrator.domain.model.status.MSAStatus;
import com.example.orderorchestrator.domain.model.status.OrderSagaStatus;

import java.time.LocalDateTime;

public class OutboxMessage {

    private final String orderId;               // 주문 ID
    private final String payload;               // 메시지 payload(JSON)

    private MSAStatus couponStatus;
    private MSAStatus pointStatus;
    private MSAStatus orderStatus;
    private MSAStatus paymentStatus;

    private OrderSagaStatus sagaStatus;

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OutboxMessage(
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

    // Outbox 최초 생성 시 사용하는 팩토리
    public static OutboxMessage initial(
            String orderId,
            String payload,
            MSAStatus couponStatus,
            MSAStatus pointStatus
    ) {
        return new OutboxMessage(
                orderId,
                payload,
                couponStatus,
                pointStatus,
                MSAStatus.InProgress,  // 주문 MSA 요청 시작
                MSAStatus.InProgress,  // 결제 MSA 요청 시작
                OrderSagaStatus.InProgress,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // getter
    public String orderId() { return orderId; }
    public String payload() { return payload; }

    public MSAStatus couponStatus() { return couponStatus; }
    public MSAStatus pointStatus() { return pointStatus; }
    public MSAStatus orderStatus() { return orderStatus; }
    public MSAStatus paymentStatus() { return paymentStatus; }

    public OrderSagaStatus sagaStatus() { return sagaStatus; }

    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime updatedAt() { return updatedAt; }

    // 상태 변경 로직
    public void updateSagaStatus(OrderSagaStatus newStatus) {
        this.sagaStatus = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public void markCouponStatus(MSAStatus status) {
        this.couponStatus = status;
        this.updatedAt = LocalDateTime.now();
    }

    public void markPointStatus(MSAStatus status) {
        this.pointStatus = status;
        this.updatedAt = LocalDateTime.now();
    }

    public void markOrderStatus(MSAStatus status) {
        this.orderStatus = status;
        this.updatedAt = LocalDateTime.now();
    }

    public void markPaymentStatus(MSAStatus status) {
        this.paymentStatus = status;
        this.updatedAt = LocalDateTime.now();
    }
}

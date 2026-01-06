package com.example.orderorchestrator.adapter.out.persistence.jpa.entity;

import com.example.common.status.OrderSagaStatus;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_saga")
public class OrderSagaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(name = "saga_id", nullable = false, unique = true)
    private String sagaId;

    @Column(name = "coupon_number")
    private String couponNumber;

    @Column(name = "point_number")
    private String pointNumber;

    @Column(name = "payment_number")
    private String paymentNumber;

    @Column(name = "payment_amount")
    private long paymentAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderSagaStatus status;

    @OneToMany(mappedBy = "orderSaga", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemJpaEntity> items = new ArrayList<>();

    protected OrderSagaJpaEntity() {
    }

    public OrderSagaJpaEntity(
            String orderId,
            String sagaId,
            String couponNumber,
            String pointNumber,
            String paymentNumber,
            long paymentAmount,
            OrderSagaStatus status
    ) {
        this.orderId = orderId;
        this.sagaId = sagaId;
        this.couponNumber = couponNumber;
        this.pointNumber = pointNumber;
        this.paymentNumber = paymentNumber;
        this.paymentAmount = paymentAmount;
        this.status = status;
    }

    public void addItem(OrderItemJpaEntity item) {
        items.add(item);
        item.setOrderSaga(this);
    }

    public Long getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getSagaId() { return sagaId; }
    public String getCouponNumber() { return couponNumber; }
    public String getPointNumber() { return pointNumber; }
    public String getPaymentNumber() { return paymentNumber; }
    public long getPaymentAmount() { return paymentAmount; }
    public OrderSagaStatus getStatus() { return status; }
    public List<OrderItemJpaEntity> getItems() { return items; }

    public void setStatus(OrderSagaStatus status) { this.status = status; }
}

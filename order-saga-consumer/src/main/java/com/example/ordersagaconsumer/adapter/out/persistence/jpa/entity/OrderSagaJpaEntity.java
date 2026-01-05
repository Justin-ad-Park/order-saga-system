package com.example.ordersagaconsumer.adapter.out.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_saga")
public class OrderSagaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(name = "coupon_number")
    private String couponNumber;

    @Column(name = "point_number")
    private String pointNumber;

    protected OrderSagaJpaEntity() {
    }

    public Long getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getCouponNumber() { return couponNumber; }
    public String getPointNumber() { return pointNumber; }
}

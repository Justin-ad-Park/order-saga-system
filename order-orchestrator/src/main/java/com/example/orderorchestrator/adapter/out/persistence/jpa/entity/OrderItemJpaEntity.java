// src/main/java/com/example/orderorchestrator/adapter/out/persistence/jpa/OrderItemJpaEntity.java
package com.example.orderorchestrator.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "order_item")
public class OrderItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_number", nullable = false)
    private String itemNumber;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_saga_id")
    private OrderSagaJpaEntity orderSaga;

    protected OrderItemJpaEntity() {
    }

    public OrderItemJpaEntity(String itemNumber, int quantity) {
        this.itemNumber = itemNumber;
        this.quantity = quantity;
    }

    void setOrderSaga(OrderSagaJpaEntity orderSaga) {
        this.orderSaga = orderSaga;
    }

    public Long getId() { return id; }
    public String getItemNumber() { return itemNumber; }
    public int getQuantity() { return quantity; }
}

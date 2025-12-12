// src/main/java/com/example/orderorchestrator/adapter/out/persistence/OrderSagaPersistenceAdapter.java
package com.example.orderorchestrator.adapter.out.persistence;

import com.example.orderorchestrator.adapter.out.persistence.jpa.entity.OrderItemJpaEntity;
import com.example.orderorchestrator.adapter.out.persistence.jpa.entity.OrderSagaJpaEntity;
import com.example.orderorchestrator.application.port.out.SaveOrderSagaPort;
import com.example.orderorchestrator.adapter.out.persistence.jpa.OrderSagaJpaRepository;
import com.example.orderorchestrator.domain.model.OrderItem;
import com.example.orderorchestrator.domain.model.OrderSaga;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class OrderSagaPersistenceAdapter implements SaveOrderSagaPort {

    private final OrderSagaJpaRepository orderSagaJpaRepository;

    public OrderSagaPersistenceAdapter(OrderSagaJpaRepository orderSagaJpaRepository) {
        this.orderSagaJpaRepository = orderSagaJpaRepository;
    }

    @Override
    public OrderSaga save(OrderSaga saga) {
        OrderSagaJpaEntity entity = new OrderSagaJpaEntity(
                saga.orderId(),
                saga.sagaId(),
                saga.couponNumber(),
                saga.paymentNumber(),
                saga.paymentAmount(),
                saga.status()
        );

        for (OrderItem item : saga.orderItems()) {
            OrderItemJpaEntity itemEntity = new OrderItemJpaEntity(
                    item.itemNumber(),
                    item.quantity()
            );
            entity.addItem(itemEntity);
        }

        OrderSagaJpaEntity saved = orderSagaJpaRepository.save(entity);

        // JPA → 도메인 역매핑 (items 는 기존 saga.orderItems() 재사용)
        return OrderSaga.create(
                saved.getOrderId(),
                saved.getSagaId(),
                saved.getCouponNumber(),
                saved.getPaymentNumber(),
                saved.getPaymentAmount(),
                saga.orderItems(),
                saved.getStatus()         // OrderSagaStatus 그대로 사용
        );
    }
}

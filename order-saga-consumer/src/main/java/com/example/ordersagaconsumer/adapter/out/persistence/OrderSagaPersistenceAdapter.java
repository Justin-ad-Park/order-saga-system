package com.example.ordersagaconsumer.adapter.out.persistence;

import com.example.ordersagaconsumer.adapter.out.persistence.jpa.OrderSagaJpaRepository;
import com.example.ordersagaconsumer.application.port.out.LoadOrderSagaPort;
import com.example.ordersagaconsumer.domain.model.OrderSagaInfo;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class OrderSagaPersistenceAdapter implements LoadOrderSagaPort {

    private final OrderSagaJpaRepository orderSagaJpaRepository;

    public OrderSagaPersistenceAdapter(OrderSagaJpaRepository orderSagaJpaRepository) {
        this.orderSagaJpaRepository = orderSagaJpaRepository;
    }

    @Override
    public Optional<OrderSagaInfo> findByOrderId(String orderId) {
        return orderSagaJpaRepository.findByOrderId(orderId)
                .map(entity -> new OrderSagaInfo(
                        entity.getOrderId(),
                        entity.getCouponNumber(),
                        entity.getPointNumber()
                ));
    }
}

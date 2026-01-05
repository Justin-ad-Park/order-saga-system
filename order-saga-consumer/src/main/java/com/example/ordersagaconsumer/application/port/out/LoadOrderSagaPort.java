package com.example.ordersagaconsumer.application.port.out;

import com.example.ordersagaconsumer.domain.model.OrderSagaInfo;
import java.util.Optional;

public interface LoadOrderSagaPort {
    Optional<OrderSagaInfo> findByOrderId(String orderId);
}

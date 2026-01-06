package com.example.ordersagaconsumer.application.port.out;

import com.example.ordersagaconsumer.domain.model.status.MSAStatus;
import com.example.ordersagaconsumer.domain.model.status.OrderSagaStatus;

public interface UpdateOutboxMessagePort {
    void updateCouponStatus(String orderId, MSAStatus status);
    void updatePointStatus(String orderId, MSAStatus status);
    void updateCompletedStatus(String orderId);
    void updateCompensatedStatus(String orderId);
    void updateSagaStatus(String orderId, OrderSagaStatus status);
}

package com.example.ordersagaconsumer.domain.model.status;

public enum OrderSagaStatus {
    InProgress,
    Reserved,
    Completed,
    Compensating,
    Compensated
}

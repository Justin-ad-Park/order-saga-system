package com.example.common.status;

public enum OrderSagaStatus {
    InProgress,
    Reserved,
    Completed,
    Failed,
    Compensating,
    Compensated
}

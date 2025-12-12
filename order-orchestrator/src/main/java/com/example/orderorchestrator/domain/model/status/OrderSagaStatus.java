package com.example.orderorchestrator.domain.model.status;

public enum OrderSagaStatus {
    InProgress,        // 관련 MSA에 예약을 진행하는 단계
    Reserved,          // 모든 예약 완료
    PaymentCompleted,  // 결제 완료
    Completed,         // 최종 완료
    Compensating,      // 보상 처리 중
    Compensated        // 보상 완료
}
package com.example.orderorchestrator.domain.model.status;

public enum MSAStatus {
    NotUsed,        // 해당 MSA가 이번 주문에 참여하지 않는 경우
    InProgress,     // 요청 중
    Reserved,       // 예약됨
    Completed,      // 확정됨
    Failed,         // 실패 (예약/결제 실패) → 주문 보상 메시지 발행
    Compensated     // 보상 처리 완료
}
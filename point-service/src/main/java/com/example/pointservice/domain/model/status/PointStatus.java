package com.example.pointservice.domain.model.status;

public enum PointStatus {
    AVAILABLE,     // 사용 가능
    RESERVED,      // 예약됨 (주문 생성 중)
    USED,          // 사용 완료 (주문 확정)
    COMPENSATED    // 보상 처리로 다시 환원됨
}
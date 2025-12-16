package com.example.couponservice.domain.model.status;

public enum CouponStatus {
    AVAILABLE,     // 사용 가능
    RESERVED,      // 예약됨 (주문 생성 중)
    USED,          // 사용 완료 (주문 확정)
    COMPENSATED    // 보상 처리로 다시 환원됨
}
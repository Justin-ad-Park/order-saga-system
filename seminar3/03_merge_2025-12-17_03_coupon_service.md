# 03. 03_coupon_service -> main

## 시점
- 2025-12-17

## 비교 기준
- 직전 main 상태: `6e8df394e7b16807ccf44746fff7c934531eaeb7`
- 브랜치 tip: `3103fe4`

## 주요 변경(커밋 메시지 기반)
- ReserveCouponServiceTest Mock

## MSA + EDA + SAGA 관점 요약
- 쿠폰 서비스 변경

## 연결된 로직 흐름
- 유스케이스/서비스 처리

## 핵심 로직 스니펫(머지 시점 기준)
- `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`
```java
package com.example.couponservice.adapter.in.web;

import com.example.common.api.ApiResponse;
import com.example.couponservice.adapter.in.web.dto.request.ReserveCouponRequest;
import com.example.couponservice.adapter.in.web.dto.response.ReserveCouponResponse;
import com.example.couponservice.application.port.in.ReserveCouponUseCase;
import com.example.couponservice.domain.model.status.CouponStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final ReserveCouponUseCase reserveCouponUseCase;

    @PostMapping("/reserve")
    public ApiResponse<ReserveCouponResponse> reserveCoupon(@RequestBody ReserveCouponRequest request) {
        reserveCouponUseCase.reserve(request.couponNumber(), request.orderId());

        // 지금은 단순히 RESERVED 라고 응답만 내려줌
        ReserveCouponResponse response = new ReserveCouponResponse(
                request.couponNumber(),
                CouponStatus.RESERVED.name()
        );
        return ApiResponse.success(response);
    }
}
```
- `coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponService.java`
```java
package com.example.couponservice.application.service;

import com.example.couponservice.application.port.in.ReserveCouponUseCase;
import com.example.couponservice.application.port.out.LoadCouponPort;
import com.example.couponservice.application.port.out.SaveCouponPort;
import com.example.couponservice.domain.model.Coupon;
import com.example.couponservice.domain.model.status.CouponStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class ReserveCouponService implements ReserveCouponUseCase {

    private final LoadCouponPort loadCouponPort;
    private final SaveCouponPort saveCouponPort;

    @Override
    public void reserve(String couponNumber, String orderId) {
        Coupon coupon = loadCouponPort.loadCoupon(couponNumber)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다: " + couponNumber));

        if (!coupon.isAvailable()) {
            throw new IllegalStateException("예약 불가능한 쿠폰입니다: " + couponNumber);
        }

        // 지금은 간단히 status만 RESERVED로 변경한 새 인스턴스를 만든다고 가정
        Coupon reserved = new Coupon(
                coupon.couponNumber(),
                CouponStatus.RESERVED,
                coupon.issuedAt(),
                coupon.expiredAt()
        );

        saveCouponPort.save(reserved);
    }
}
```
- `coupon-service/src/test/java/com/example/couponservice/application/service/ReserveCouponServiceTest.java`
```java
package com.example.couponservice.application.service;

import com.example.couponservice.application.port.out.LoadCouponPort;
import com.example.couponservice.application.port.out.SaveCouponPort;
import com.example.couponservice.domain.model.Coupon;
import com.example.couponservice.domain.model.status.CouponStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class ReserveCouponServiceTest {

    private LoadCouponPort loadCouponPort;
    private SaveCouponPort saveCouponPort;
    private ReserveCouponService reserveCouponService;

    @BeforeEach
    void setUp() {
        loadCouponPort = mock(LoadCouponPort.class);
        saveCouponPort = mock(SaveCouponPort.class);
        reserveCouponService = new ReserveCouponService(loadCouponPort, saveCouponPort);
    }

    @Test
    void reserve_shouldChangeStatusToReserved_andSave() {
        // given
        String couponNumber = "CPN-001";
        LocalDateTime now = LocalDateTime.now();
        Coupon availableCoupon = new Coupon(couponNumber, CouponStatus.AVAILABLE, now.minusDays(1), now.plusDays(1));

        when(loadCouponPort.loadCoupon(couponNumber)).thenReturn(Optional.of(availableCoupon));

        // when
        reserveCouponService.reserve(couponNumber, "ORD-001");

        // then
        verify(loadCouponPort, times(1)).loadCoupon(couponNumber);
        verify(saveCouponPort, times(1)).save(argThat(saved ->
                saved.couponNumber().equals(couponNumber)
                        && saved.status() == CouponStatus.RESERVED
        ));
    }

    @Test
    void reserve_shouldThrow_ifCouponNotFound() {
        String couponNumber = "UNKNOWN";
        when(loadCouponPort.loadCoupon(couponNumber)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reserveCouponService.reserve(couponNumber, "ORD-001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("쿠폰을 찾을 수 없습니다");

        verify(saveCouponPort, never()).save(any());
    }

    @Test
    void reserve_shouldThrow_ifCouponNotAvailable() {
        String couponNumber = "CPN-002";
        LocalDateTime now = LocalDateTime.now();
        Coupon reserved = new Coupon(couponNumber, CouponStatus.RESERVED, now.minusDays(1), now.plusDays(1));
        when(loadCouponPort.loadCoupon(couponNumber)).thenReturn(Optional.of(reserved));

        assertThatThrownBy(() -> reserveCouponService.reserve(couponNumber, "ORD-002"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("예약 불가능한 쿠폰");

        verify(saveCouponPort, never()).save(any());
    }
}
```

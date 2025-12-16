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

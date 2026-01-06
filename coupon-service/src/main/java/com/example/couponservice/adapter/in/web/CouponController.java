package com.example.couponservice.adapter.in.web;

import com.example.common.api.ApiResponse;
import com.example.couponservice.adapter.in.web.dto.request.CompensateCouponRequest;
import com.example.couponservice.adapter.in.web.dto.request.ConfirmCouponRequest;
import com.example.couponservice.adapter.in.web.dto.request.ReserveCouponRequest;
import com.example.couponservice.adapter.in.web.dto.response.CompensateCouponResponse;
import com.example.couponservice.adapter.in.web.dto.response.ConfirmCouponResponse;
import com.example.couponservice.adapter.in.web.dto.response.ReserveCouponResponse;
import com.example.couponservice.application.port.in.CompensateCouponUseCase;
import com.example.couponservice.application.port.in.ConfirmCouponUseCase;
import com.example.couponservice.application.port.in.ReserveCouponUseCase;
import com.example.couponservice.domain.model.status.CouponStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final ReserveCouponUseCase reserveCouponUseCase;
    private final ConfirmCouponUseCase confirmCouponUseCase;
    private final CompensateCouponUseCase compensateCouponUseCase;

    @PostMapping("/reserve")
    public ApiResponse<ReserveCouponResponse> reserveCoupon(@RequestBody ReserveCouponRequest request) {
        reserveCouponUseCase.reserve(request.couponNumber(), request.orderId());

        return ApiResponse.success(buildReserveResponse(request.couponNumber(), CouponStatus.RESERVED));
    }

    @PostMapping("/confirm")
    public ApiResponse<ConfirmCouponResponse> confirmCoupon(@RequestBody ConfirmCouponRequest request) {
        confirmCouponUseCase.confirm(request.couponNumber(), request.orderId());
        return ApiResponse.success(buildConfirmResponse(request.couponNumber(), CouponStatus.USED));
    }

    @PostMapping("/compensate")
    public ApiResponse<CompensateCouponResponse> compensateCoupon(@RequestBody CompensateCouponRequest request) {
        compensateCouponUseCase.compensateCoupon(request.couponNumber(), request.orderId());
        return ApiResponse.success(buildCompensateResponse(request.couponNumber(), CouponStatus.AVAILABLE));
    }

    private ReserveCouponResponse buildReserveResponse(String couponNumber, CouponStatus status) {
        return new ReserveCouponResponse(
                couponNumber,
                status.name()
        );
    }

    private ConfirmCouponResponse buildConfirmResponse(String couponNumber, CouponStatus status) {
        return new ConfirmCouponResponse(
                couponNumber,
                status.name()
        );
    }

    private CompensateCouponResponse buildCompensateResponse(String couponNumber, CouponStatus status) {
        return new CompensateCouponResponse(
                couponNumber,
                status.name()
        );
    }
}

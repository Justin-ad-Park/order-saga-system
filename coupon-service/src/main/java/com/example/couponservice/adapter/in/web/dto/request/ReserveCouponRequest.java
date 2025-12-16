package com.example.couponservice.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReserveCouponRequest(
        @NotBlank String couponNumber,
        @NotBlank String orderId
) {}

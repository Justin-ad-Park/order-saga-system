package com.example.pointservice.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReservePointRequest(
        @NotBlank String pointNumber,
        @NotBlank String orderId
) {}

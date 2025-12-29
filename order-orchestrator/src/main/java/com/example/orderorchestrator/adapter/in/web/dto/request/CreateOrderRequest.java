// src/main/java/com/example/orderorchestrator/adapter/in/web/dto/request/CreateOrderRequest.java
package com.example.orderorchestrator.adapter.in.web.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
    @NotBlank
    String couponNumber,

    @NotBlank
    String pointNumber,

    @NotBlank
    String paymentNumber,

        @NotNull
        @Min(1)
        Long paymentAmount,

        @NotEmpty
        List<OrderItemRequest> orderItems
) {
}

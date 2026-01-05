package com.example.pointservice.adapter.in.web.dto.request;

public record ConfirmPointRequest(
        String pointNumber,
        String orderId
) {
}

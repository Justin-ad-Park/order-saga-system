package com.example.pointservice.adapter.in.web.dto.request;

public record CompensatePointRequest(
        String pointNumber,
        String orderId
) {
}

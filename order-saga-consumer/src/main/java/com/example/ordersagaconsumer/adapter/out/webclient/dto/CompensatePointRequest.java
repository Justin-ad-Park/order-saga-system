package com.example.ordersagaconsumer.adapter.out.webclient.dto;

public record CompensatePointRequest(
        String pointNumber,
        String orderId
) {
}

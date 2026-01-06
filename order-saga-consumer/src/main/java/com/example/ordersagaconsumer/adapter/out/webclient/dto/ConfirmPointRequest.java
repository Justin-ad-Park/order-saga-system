package com.example.ordersagaconsumer.adapter.out.webclient.dto;

public record ConfirmPointRequest(
        String pointNumber,
        String orderId
) {
}

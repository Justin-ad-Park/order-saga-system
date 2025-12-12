package com.example.orderorchestrator.domain.model;

import com.example.orderorchestrator.domain.model.status.OrderSagaStatus;

import java.util.List;

public class OrderSaga {

    private final String orderId;
    private final String sagaId;

    private final String couponNumber;      //쿠폰번호
    private final String paymentNumber;     //결제승인번호(개념만)
    private final long paymentAmount;       //결제금액

    private final List<OrderItem> orderItems;   //상품목록(상품번호, 수량)

    private OrderSagaStatus status;

    private OrderSaga(
            String orderId,
            String sagaId,
            String couponNumber,
            String paymentNumber,
            long paymentAmount,
            List<OrderItem> orderItems,
            OrderSagaStatus status
    ) {
        this.orderId = orderId;
        this.sagaId = sagaId;
        this.couponNumber = couponNumber;
        this.paymentNumber = paymentNumber;
        this.paymentAmount = paymentAmount;
        this.orderItems = List.copyOf(orderItems);
        this.status = status;
    }

    public static OrderSaga create(
            String orderId,
            String sagaId,
            String couponNumber,
            String paymentNumber,
            long paymentAmount,
            List<OrderItem> orderItems,
            OrderSagaStatus status
    ) {
        return new OrderSaga(
                orderId,
                sagaId,
                couponNumber,
                paymentNumber,
                paymentAmount,
                orderItems,
                status
        );
    }

    public String orderId() {
        return orderId;
    }

    public String sagaId() {
        return sagaId;
    }

    public String couponNumber() {
        return couponNumber;
    }

    public String paymentNumber() {
        return paymentNumber;
    }

    public long paymentAmount() {
        return paymentAmount;
    }

    public List<OrderItem> orderItems() {
        return orderItems;
    }

    public OrderSagaStatus status() {
        return status;
    }

    public void changeStatus(OrderSagaStatus newStatus) {
        this.status = newStatus;
    }
}
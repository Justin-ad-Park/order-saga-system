package com.example.ordersagaconsumer.application.service;

import com.example.ordersagaconsumer.application.port.in.ProcessOrderSagaEventUseCase;
import com.example.ordersagaconsumer.application.port.out.CouponServicePort;
import com.example.ordersagaconsumer.application.port.out.LoadOrderSagaPort;
import com.example.ordersagaconsumer.application.port.out.PointServicePort;
import com.example.ordersagaconsumer.application.port.out.UpdateOrderSagaStatusPort;
import com.example.ordersagaconsumer.application.port.out.UpdateOutboxMessagePort;
import com.example.ordersagaconsumer.domain.model.OrderSagaInfo;
import com.example.ordersagaconsumer.domain.model.status.MSAStatus;
import com.example.ordersagaconsumer.domain.model.status.OrderSagaStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProcessOrderSagaEventService implements ProcessOrderSagaEventUseCase {

    private final LoadOrderSagaPort loadOrderSagaPort;
    private final CouponServicePort couponServicePort;
    private final PointServicePort pointServicePort;
    private final UpdateOutboxMessagePort updateOutboxMessagePort;
    private final UpdateOrderSagaStatusPort updateOrderSagaStatusPort;

    public ProcessOrderSagaEventService(
            LoadOrderSagaPort loadOrderSagaPort,
            CouponServicePort couponServicePort,
            PointServicePort pointServicePort,
            UpdateOutboxMessagePort updateOutboxMessagePort,
            UpdateOrderSagaStatusPort updateOrderSagaStatusPort
    ) {
        this.loadOrderSagaPort = loadOrderSagaPort;
        this.couponServicePort = couponServicePort;
        this.pointServicePort = pointServicePort;
        this.updateOutboxMessagePort = updateOutboxMessagePort;
        this.updateOrderSagaStatusPort = updateOrderSagaStatusPort;
    }

    @Override
    public void process(String orderId, String status) {
        if (orderId == null || orderId.isBlank()) {
            System.out.println("### OrderSaga lookup skipped ### : empty orderId");
            return;
        }

        OrderSagaInfo info = loadOrderSagaPort.findByOrderId(orderId)
                .orElse(null);

        if (info == null) {
            System.out.println("### OrderSaga not found ### : orderId=" + orderId
                    + " status=" + status);
            return;
        }

        System.out.println("### OrderSaga details ### : orderId=" + orderId
                + " status=" + status
                + " couponNumber=" + info.couponNumber()
                + " pointNumber=" + info.pointNumber());

        OrderSagaStatus sagaStatus = parseSagaStatus(status);
        if (sagaStatus == null) {
            System.out.println("### OrderSaga status skipped ### : unsupported status=" + status);
            return;
        }

        if (sagaStatus == OrderSagaStatus.Reserved) {
            handleConfirm(orderId, info);
            return;
        }

        if (sagaStatus == OrderSagaStatus.Compensating) {
            handleCompensate(orderId, info);
        }
    }

    private void handleConfirm(String orderId, OrderSagaInfo info) {
        boolean couponNeeded = StringUtils.hasText(info.couponNumber());
        boolean pointNeeded = StringUtils.hasText(info.pointNumber());

        boolean couponOk = true;
        boolean pointOk = true;

        if (couponNeeded) {
            couponOk = couponServicePort.confirm(info.couponNumber(), orderId);
            updateOutboxMessagePort.updateCouponStatus(
                    orderId,
                    couponOk ? MSAStatus.Completed : MSAStatus.Failed
            );
        }

        if (pointNeeded) {
            pointOk = pointServicePort.confirm(info.pointNumber(), orderId);
            updateOutboxMessagePort.updatePointStatus(
                    orderId,
                    pointOk ? MSAStatus.Completed : MSAStatus.Failed
            );
        }

        if (couponOk && pointOk) {
            updateOutboxMessagePort.updateSagaStatus(orderId, OrderSagaStatus.Completed);
            updateOrderSagaStatusPort.updateStatus(orderId, OrderSagaStatus.Completed);
        }
    }

    private void handleCompensate(String orderId, OrderSagaInfo info) {
        boolean couponNeeded = StringUtils.hasText(info.couponNumber());
        boolean pointNeeded = StringUtils.hasText(info.pointNumber());

        boolean couponOk = true;
        boolean pointOk = true;

        if (couponNeeded) {
            couponOk = couponServicePort.compensate(info.couponNumber(), orderId);
            updateOutboxMessagePort.updateCouponStatus(
                    orderId,
                    couponOk ? MSAStatus.Compensated : MSAStatus.Failed
            );
        }

        if (pointNeeded) {
            pointOk = pointServicePort.compensate(info.pointNumber(), orderId);
            updateOutboxMessagePort.updatePointStatus(
                    orderId,
                    pointOk ? MSAStatus.Compensated : MSAStatus.Failed
            );
        }

        if (couponOk && pointOk) {
            updateOutboxMessagePort.updateSagaStatus(orderId, OrderSagaStatus.Compensated);
            updateOrderSagaStatusPort.updateStatus(orderId, OrderSagaStatus.Compensated);
        }
    }

    private OrderSagaStatus parseSagaStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return OrderSagaStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

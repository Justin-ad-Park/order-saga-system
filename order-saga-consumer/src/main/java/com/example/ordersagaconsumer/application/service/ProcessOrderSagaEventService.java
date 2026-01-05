package com.example.ordersagaconsumer.application.service;

import com.example.ordersagaconsumer.application.port.in.ProcessOrderSagaEventUseCase;
import com.example.ordersagaconsumer.application.port.out.LoadOrderSagaPort;
import com.example.ordersagaconsumer.domain.model.OrderSagaInfo;
import org.springframework.stereotype.Service;

@Service
public class ProcessOrderSagaEventService implements ProcessOrderSagaEventUseCase {

    private final LoadOrderSagaPort loadOrderSagaPort;

    public ProcessOrderSagaEventService(LoadOrderSagaPort loadOrderSagaPort) {
        this.loadOrderSagaPort = loadOrderSagaPort;
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
    }
}

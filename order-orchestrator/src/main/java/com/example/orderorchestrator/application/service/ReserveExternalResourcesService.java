package com.example.orderorchestrator.application.service;

import com.example.common.status.MSAStatus;
import com.example.orderorchestrator.application.port.in.UpdateOutboxMessageUseCase;
import com.example.orderorchestrator.application.port.out.ReserveCouponPort;
import com.example.orderorchestrator.application.port.out.ReservePointPort;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ReserveExternalResourcesService {

    private final ReserveCouponPort reserveCouponPort;
    private final ReservePointPort reservePointPort;
    private final UpdateOutboxMessageUseCase updateOutboxMessageUseCase;

    public Mono<Void> reserveExternalResources(String orderId, String couponNumber, String pointNumber) {
        List<Mono<?>> calls = new ArrayList<>();
        // Reserve independently; failures are collected and surfaced after all attempts.
        if (StringUtils.hasText(couponNumber)) {
            calls.add(reserveCoupon(couponNumber, orderId));
        }
        if (StringUtils.hasText(pointNumber)) {
            calls.add(reservePoint(pointNumber, orderId));
        }
        if (calls.isEmpty()) {
            return Mono.empty();
        }
        return Mono.whenDelayError(calls).then();
    }

    private Mono<Void> reserveCoupon(String couponNumber, String orderId) {
        // Update outbox status to reflect external reservation outcome.
        return reserveCouponPort.reserveCoupon(couponNumber, orderId)
                .doOnSuccess(ignored -> updateOutboxMessageUseCase.updateCouponStatus(orderId, MSAStatus.Reserved))
                .onErrorResume(ex -> {
                    updateOutboxMessageUseCase.updateCouponStatus(orderId, MSAStatus.Failed);
                    return Mono.error(ex);
                })
                .then();
    }

    private Mono<Void> reservePoint(String pointNumber, String orderId) {
        // Update outbox status to reflect external reservation outcome.
        return reservePointPort.reservePoint(pointNumber, orderId)
                .doOnSuccess(ignored -> updateOutboxMessageUseCase.updatePointStatus(orderId, MSAStatus.Reserved))
                .onErrorResume(ex -> {
                    updateOutboxMessageUseCase.updatePointStatus(orderId, MSAStatus.Failed);
                    return Mono.error(ex);
                })
                .then();
    }
}

package com.example.orderorchestrator.adapter.in.web;

import com.example.orderorchestrator.adapter.in.web.dto.request.CreateOrderRequest;
import com.example.orderorchestrator.adapter.in.web.dto.response.CreateOrderResponse;
import com.example.orderorchestrator.adapter.out.webclient.CouponServiceClient;
import com.example.orderorchestrator.adapter.out.webclient.PointServiceClient;
import com.example.orderorchestrator.application.port.in.CreateOrderUseCase;
import com.example.orderorchestrator.application.port.in.UpdateOutboxMessageUseCase;
import com.example.orderorchestrator.application.port.in.command.CreateOrderCommand;
import com.example.orderorchestrator.application.port.in.result.CreateOrderResult;
import com.example.orderorchestrator.domain.model.status.MSAStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderOrchestrationController {

    private final CreateOrderUseCase createOrderUseCase;
    private final CouponServiceClient couponServiceClient;
    private final PointServiceClient pointServiceClient;
    private final UpdateOutboxMessageUseCase updateOutboxMessageUseCase;

    @PostMapping
    public Mono<ResponseEntity<CreateOrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        CreateOrderCommand command = mapToCommand(request);
        CreateOrderResult result = createOrderUseCase.createOrder(command);

        return reserveExternalResources(request, result)
                .thenReturn(ResponseEntity.ok(mapToResponse(result)));
    }

    private CreateOrderCommand mapToCommand(CreateOrderRequest request) {
        var orderItems = request.orderItems().stream()
                .map(item -> new CreateOrderCommand.OrderItemCommand(
                        item.itemNumber(),
                        item.quantity()
                ))
                .collect(Collectors.toList());

        return new CreateOrderCommand(
                request.couponNumber(),
                request.pointNumber(),
                request.paymentNumber(),
                request.paymentAmount(),
                orderItems
        );
    }

    private CreateOrderResponse mapToResponse(CreateOrderResult result) {
        return CreateOrderResponse.of(
                result.orderId(),
                result.sagaId(),
                result.status()
        );
    }

    private Mono<Void> reserveExternalResources(CreateOrderRequest request, CreateOrderResult result) {
        List<Mono<?>> calls = new ArrayList<>();
        if (StringUtils.hasText(request.couponNumber())) {
            calls.add(reserveCoupon(request.couponNumber(), result.orderId()));
        }
        if (StringUtils.hasText(request.pointNumber())) {
            calls.add(reservePoint(request.pointNumber(), result.orderId()));
        }
        if (calls.isEmpty()) {
            return Mono.empty();
        }
        return Mono.whenDelayError(calls).then();
    }

    private Mono<Void> reserveCoupon(String couponNumber, String orderId) {
        return couponServiceClient.reserveCoupon(couponNumber, orderId)
                .doOnSuccess(response -> updateOutboxMessageUseCase.updateCouponStatus(orderId, MSAStatus.Reserved))
                .onErrorResume(ex -> {
                    updateOutboxMessageUseCase.updateCouponStatus(orderId, MSAStatus.Failed);
                    return Mono.error(ex);
                })
                .then();
    }

    private Mono<Void> reservePoint(String pointNumber, String orderId) {
        return pointServiceClient.reservePoint(pointNumber, orderId)
                .doOnSuccess(response -> updateOutboxMessageUseCase.updatePointStatus(orderId, MSAStatus.Reserved))
                .onErrorResume(ex -> {
                    updateOutboxMessageUseCase.updatePointStatus(orderId, MSAStatus.Failed);
                    return Mono.error(ex);
                })
                .then();
    }
}

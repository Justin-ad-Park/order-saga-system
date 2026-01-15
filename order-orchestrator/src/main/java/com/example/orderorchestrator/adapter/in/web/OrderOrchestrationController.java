package com.example.orderorchestrator.adapter.in.web;

import com.example.orderorchestrator.adapter.in.web.dto.request.CreateOrderRequest;
import com.example.orderorchestrator.adapter.in.web.dto.response.CreateOrderResponse;
import com.example.orderorchestrator.application.port.in.CreateOrderUseCase;
import com.example.orderorchestrator.application.port.in.UpdateOrderSagaStatusUseCase;
import com.example.orderorchestrator.application.port.in.UpdateOutboxMessageUseCase;
import com.example.orderorchestrator.application.port.in.command.CreateOrderCommand;
import com.example.orderorchestrator.application.port.in.result.CreateOrderResult;
import com.example.orderorchestrator.application.service.OrderSagaEventService;
import com.example.orderorchestrator.application.service.ReserveExternalResourcesService;
import com.example.orderorchestrator.domain.event.OrderSagaEventType;
import com.example.common.status.OrderSagaStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderOrchestrationController {

    private final CreateOrderUseCase createOrderUseCase;
    private final ReserveExternalResourcesService reserveExternalResourcesService;
    private final UpdateOutboxMessageUseCase updateOutboxMessageUseCase;
    private final UpdateOrderSagaStatusUseCase updateOrderSagaStatusUseCase;
    private final OrderSagaEventService orderSagaEventService;

    @PostMapping
    public Mono<ResponseEntity<CreateOrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        CreateOrderCommand command = mapToCommand(request);
        CreateOrderResult result = createOrderUseCase.createOrder(command);

        return reserveExternalResourcesService.reserveExternalResources(
                        result.orderId(),
                        request.couponNumber(),
                        request.pointNumber()
                )
                .then(Mono.fromRunnable(() -> {
                    updateSagaStatus(result.orderId(), OrderSagaStatus.Reserved);
                    publishSagaEvent(result, OrderSagaStatus.Reserved, OrderSagaEventType.RESERVE_SUCCEEDED);
                }))
                .onErrorResume(ex -> {
                    updateSagaStatus(result.orderId(), OrderSagaStatus.Compensating);
                    publishSagaEvent(result, OrderSagaStatus.Compensating, OrderSagaEventType.RESERVE_FAILED);
                    return Mono.error(ex);
                })
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

    private void updateSagaStatus(String orderId, OrderSagaStatus status) {
        updateOrderSagaStatusUseCase.updateStatus(orderId, status);
        updateOutboxMessageUseCase.updateSagaStatus(orderId, status);
    }

    private void publishSagaEvent(CreateOrderResult result, OrderSagaStatus status, OrderSagaEventType type) {
        orderSagaEventService.publish(result.orderId(), result.sagaId(), status, type);
    }
}

package com.example.orderorchestrator.adapter.in.web;

import com.example.orderorchestrator.adapter.in.web.dto.request.CreateOrderRequest;
import com.example.orderorchestrator.adapter.in.web.dto.response.CreateOrderResponse;
import com.example.orderorchestrator.adapter.out.webclient.CouponServiceClient;
import com.example.orderorchestrator.application.port.in.command.CreateOrderCommand;
import com.example.orderorchestrator.application.port.in.result.CreateOrderResult;
import com.example.orderorchestrator.application.port.in.CreateOrderUseCase;
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
    private final CouponServiceClient couponServiceClient;

    @PostMapping
    public Mono<ResponseEntity<CreateOrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {

        CreateOrderCommand command = mapToCommand(request);
        CreateOrderResult result = createOrderUseCase.createOrder(command);

        CreateOrderResponse response = CreateOrderResponse.of(
                result.orderId(),
                result.sagaId(),
                result.status()
        );

        return couponServiceClient.reserveCoupon(request.couponNumber(), result.orderId())
                .thenReturn(ResponseEntity.ok(response));
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
                request.paymentNumber(),
                request.paymentAmount(),
                orderItems
        );
    }
}

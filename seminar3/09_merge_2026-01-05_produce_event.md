# 09. produce_event -> main

## 시점
- 2026-01-05

## 비교 기준
- 직전 main 상태: `ea2864873ba18a2a5b92faecbb8a1beb5889d96c`
- 브랜치 tip: `9bc1014`

## 주요 변경(커밋 메시지 기반)
- 한방에 실행, 종료 스크립트 설명 추가

## MSA + EDA + SAGA 관점 요약
- K8S/Kafka 배포 및 운영 스크립트
- 이벤트 발행/소비 확인 스크립트 활용
- OrderSagaEvent Kafka 발행 어댑터 추가

## 연결된 로직 흐름
- 인프라/배포 준비 -> 이벤트 발행 -> 이벤트 발행/소비 테스트

## 핵심 로직 스니펫(머지 시점 기준)
- `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/kafka/OrderSagaEventKafkaPublisher.java`
```java
package com.example.orderorchestrator.adapter.out.kafka;

import com.example.orderorchestrator.application.port.out.OrderSagaEventPublisher;
import com.example.orderorchestrator.domain.event.OrderSagaEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderSagaEventKafkaPublisher implements OrderSagaEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(OrderSagaEventKafkaPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public OrderSagaEventKafkaPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${order.saga.events.topic:order-saga-events}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publish(OrderSagaEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, event.orderId(), payload);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize OrderSagaEvent: orderId={}", event.orderId(), ex);
        }
    }
}
```


## 목표
오케스트레이터가 분산 호출을 묶어 사가를 시작하는 방식을 이해한다.

## 핵심 흐름
1) 주문/사가 생성
2) 쿠폰/포인트 예약 병렬 호출
3) 사가 상태 업데이트 + 이벤트 발행

## 오케스트레이터 진입점
`order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`
```java
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

```

## 분산 예약 처리
`order-orchestrator/src/main/java/com/example/orderorchestrator/application/service/ReserveExternalResourcesService.java`
```java
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

```
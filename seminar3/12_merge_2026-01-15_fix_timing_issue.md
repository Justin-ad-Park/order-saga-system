# 12. fix_timing_issue -> main

## 시점
- 2026-01-15

## 비교 기준
- 직전 main 상태: `f64daf3ec565e1d17dd14cd578ab8e4a907df10c`
- 브랜치 tip: `bfa985f`

## 주요 변경(커밋 메시지 기반)
- 타이밍 이슈 작업 완료

## MSA + EDA + SAGA 관점 요약
- 오케스트레이터 흐름 추가/수정
- 공통 모듈 추가/수정

## 연결된 로직 흐름
- API 요청 수신 -> 유스케이스/서비스 처리 -> 외부 서비스 호출/연동

## 핵심 로직 스니펫(머지 시점 기준)
## coupon_reservation 테이블로 타이밍 이슈 해결
- 보상 요청이 먼저 도착해도 `CANCELLED` 상태를 기록해 이후 예약 요청을 무시한다.
- 예약 성공 시 `RESERVED`, 보상 시 `CANCELLED`를 기록해 순서 뒤바뀜을 흡수한다.

- `coupon-service/src/main/resources/coupon_schema.sql`
```sql
CREATE TABLE IF NOT EXISTS coupon_reservation (
    order_id VARCHAR(255) PRIMARY KEY,
    coupon_number VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```
- `coupon-service/src/main/java/com/example/couponservice/domain/model/CouponReservation.java`
```java
package com.example.couponservice.domain.model;

import com.example.couponservice.domain.model.status.ReservationStatus;

public class CouponReservation {

    private final String orderId;
    private final String couponNumber;
    private final ReservationStatus status;

    public CouponReservation(String orderId, String couponNumber, ReservationStatus status) {
        this.orderId = orderId;
        this.couponNumber = couponNumber;
        this.status = status;
    }

    public String orderId() { return orderId; }
    public String couponNumber() { return couponNumber; }
    public ReservationStatus status() { return status; }
}
```
- `coupon-service/src/main/java/com/example/couponservice/domain/model/status/ReservationStatus.java`
```java
package com.example.couponservice.domain.model.status;

public enum ReservationStatus {
    RESERVED,
    CANCELLED
}
```
- `coupon-service/src/main/java/com/example/couponservice/adapter/out/persistence/CouponReservationPersistenceAdapter.java`
```java
package com.example.couponservice.adapter.out.persistence;

import com.example.couponservice.adapter.out.persistence.jpa.CouponReservationJpaEntity;
import com.example.couponservice.adapter.out.persistence.jpa.CouponReservationJpaRepository;
import com.example.couponservice.application.port.out.LoadCouponReservationPort;
import com.example.couponservice.application.port.out.SaveCouponReservationPort;
import com.example.couponservice.domain.model.CouponReservation;
import com.example.couponservice.domain.model.status.ReservationStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class CouponReservationPersistenceAdapter implements LoadCouponReservationPort, SaveCouponReservationPort {

    private final CouponReservationJpaRepository couponReservationJpaRepository;

    public CouponReservationPersistenceAdapter(CouponReservationJpaRepository couponReservationJpaRepository) {
        this.couponReservationJpaRepository = couponReservationJpaRepository;
    }

    @Override
    public Optional<CouponReservation> loadReservation(String orderId) {
        return couponReservationJpaRepository.findById(orderId)
                .map(entity -> new CouponReservation(
                        entity.getOrderId(),
                        entity.getCouponNumber(),
                        entity.getStatus()
                ));
    }

    @Override
    public CouponReservation saveReservation(CouponReservation reservation) {
        LocalDateTime now = LocalDateTime.now();
        CouponReservationJpaEntity entity = couponReservationJpaRepository.findById(reservation.orderId())
                .map(existing -> new CouponReservationJpaEntity(
                        existing.getOrderId(),
                        reservation.couponNumber(),
                        reservation.status(),
                        existing.getCreatedAt(),
                        now
                ))
                .orElseGet(() -> new CouponReservationJpaEntity(
                        reservation.orderId(),
                        reservation.couponNumber(),
                        reservation.status(),
                        now,
                        now
                ));

        CouponReservationJpaEntity saved = couponReservationJpaRepository.save(entity);
        return new CouponReservation(
                saved.getOrderId(),
                saved.getCouponNumber(),
                saved.getStatus()
        );
    }
}
```
- `coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponService.java`
```java
package com.example.couponservice.application.service;

import com.example.couponservice.application.port.in.CompensateCouponUseCase;
import com.example.couponservice.application.port.in.ConfirmCouponUseCase;
import com.example.couponservice.application.port.in.ReserveCouponUseCase;
import com.example.couponservice.application.port.out.LoadCouponPort;
import com.example.couponservice.application.port.out.LoadCouponReservationPort;
import com.example.couponservice.application.port.out.SaveCouponPort;
import com.example.couponservice.application.port.out.SaveCouponReservationPort;
import com.example.couponservice.domain.model.Coupon;
import com.example.couponservice.domain.model.CouponReservation;
import com.example.couponservice.domain.model.status.CouponStatus;
import com.example.couponservice.domain.model.status.ReservationStatus;
import jakarta.transaction.Transactional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class ReserveCouponService implements ReserveCouponUseCase, ConfirmCouponUseCase, CompensateCouponUseCase {

    private final LoadCouponPort loadCouponPort;
    private final SaveCouponPort saveCouponPort;
    private final LoadCouponReservationPort loadCouponReservationPort;
    private final SaveCouponReservationPort saveCouponReservationPort;

    @Override
    public void reserve(String couponNumber, String orderId) {
        // 이미 보상 처리된 주문이면 예약 진행하지 않음
        if (isReservationCancelled(orderId)) {
            return;
        }

        //이미 예약된 주문이면 예약 진행하지 않음
        verifyReservationNotAlreadyReserved(orderId);
        
        updateStatus(couponNumber, CouponStatus.RESERVED, this::validateReservable);
        saveCouponReservationPort.saveReservation(new CouponReservation(
                orderId,
                couponNumber,
                ReservationStatus.RESERVED
        ));
    }

    @Override
    public void compensateCoupon(String couponNumber, String orderId) {
        Coupon coupon = loadCouponPort.loadCoupon(couponNumber)
                .orElse(null);
        if (coupon == null) {
            saveReservationCancelled(orderId, couponNumber);
            return;
        }
        if (coupon.status() == CouponStatus.USED) {
            throw new IllegalStateException("보상 불가능한 쿠폰입니다: " + coupon.couponNumber());
        }

        saveReservationCancelled(orderId, couponNumber);
        if (coupon.status() != CouponStatus.RESERVED) {
            return;
        }

        Coupon updated = new Coupon(
                coupon.couponNumber(),
                CouponStatus.AVAILABLE,
                coupon.issuedAt(),
                coupon.expiredAt()
        );
        saveCouponPort.save(updated);
    }

    private boolean isReservationCancelled(String orderId) {
        return loadCouponReservationPort.loadReservation(orderId)
                .map(reservation -> reservation.status() == ReservationStatus.CANCELLED)
                .orElse(false);
    }

    private void verifyReservationNotAlreadyReserved(String orderId) {
        loadCouponReservationPort.loadReservation(orderId)
                .filter(reservation -> reservation.status() == ReservationStatus.RESERVED)
                .ifPresent(reservation -> {
                    throw new IllegalStateException("이미 예약된 주문입니다: " + reservation.orderId());
                });
    }

    private void saveReservationCancelled(String orderId, String couponNumber) {
        saveCouponReservationPort.saveReservation(new CouponReservation(
                orderId,
                couponNumber,
                ReservationStatus.CANCELLED
        ));
    }

    private void updateStatus(
            String couponNumber,
            CouponStatus targetStatus,
            Consumer<Coupon> validator
    ) {
        Coupon coupon = loadCouponPort.loadCoupon(couponNumber)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다: " + couponNumber));

        validator.accept(coupon);

        Coupon updated = new Coupon(
                coupon.couponNumber(),
                targetStatus,
                coupon.issuedAt(),
                coupon.expiredAt()
        );

        saveCouponPort.save(updated);
    }

    private void validateReservable(Coupon coupon) {
        if (!coupon.isAvailable()) {
            throw new IllegalStateException("예약 불가능한 쿠폰입니다: " + coupon.couponNumber());
        }
    }
}
```
- `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`
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
- `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/webclient/CouponServiceClient.java`
```java
package com.example.orderorchestrator.adapter.out.webclient;

import com.example.orderorchestrator.adapter.out.webclient.dto.ReserveCouponRequest;
import com.example.orderorchestrator.adapter.out.webclient.dto.ReserveCouponResponse;
import com.example.orderorchestrator.adapter.out.webclient.dto.WebApiResponse;
import com.example.orderorchestrator.application.port.out.ReserveCouponPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class CouponServiceClient implements ReserveCouponPort {

    private final WebClient webClient;

    public CouponServiceClient(
            WebClient.Builder builder,
            @Value("${external.coupon.base-url}") String baseUrl
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public Mono<Void> reserveCoupon(String couponNumber, String orderId) {
        ReserveCouponRequest request = new ReserveCouponRequest(couponNumber, orderId);

        return webClient.post()
                .uri("/api/v1/coupons/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<WebApiResponse<ReserveCouponResponse>>() {})
                .flatMap(response -> {
                    ReserveCouponResponse data = response.getData();
                    if (data == null) {
                        return Mono.error(new IllegalStateException("Reserve coupon response missing data"));
                    }
                    return Mono.just(data);
                })
                .then();
    }
}
```
- `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/webclient/PointServiceClient.java`
```java
package com.example.orderorchestrator.adapter.out.webclient;

import com.example.orderorchestrator.adapter.out.webclient.dto.ReservePointRequest;
import com.example.orderorchestrator.adapter.out.webclient.dto.ReservePointResponse;
import com.example.orderorchestrator.adapter.out.webclient.dto.WebApiResponse;
import com.example.orderorchestrator.application.port.out.ReservePointPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class PointServiceClient implements ReservePointPort {

    private final WebClient webClient;

    public PointServiceClient(
            WebClient.Builder builder,
            @Value("${external.point.base-url}") String baseUrl
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public Mono<Void> reservePoint(String pointNumber, String orderId) {
        ReservePointRequest request = new ReservePointRequest(pointNumber, orderId);

        return webClient.post()
                .uri("/api/v1/points/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<WebApiResponse<ReservePointResponse>>() {})
                .flatMap(response -> {
                    ReservePointResponse data = response.getData();
                    if (data == null) {
                        return Mono.error(new IllegalStateException("Reserve point response missing data"));
                    }
                    return Mono.just(data);
                })
                .then();
    }
}
```
- `order-orchestrator/src/main/java/com/example/orderorchestrator/application/service/OrderSagaEventService.java`
```java
package com.example.orderorchestrator.application.service;

import com.example.common.status.OrderSagaStatus;
import com.example.orderorchestrator.application.port.out.OrderSagaEventPublisher;
import com.example.orderorchestrator.domain.event.OrderSagaEvent;
import com.example.orderorchestrator.domain.event.OrderSagaEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderSagaEventService {

    private final OrderSagaEventPublisher orderSagaEventPublisher;

    public void publish(String orderId, String sagaId, OrderSagaStatus status, OrderSagaEventType type) {
        OrderSagaEvent event = new OrderSagaEvent(orderId, sagaId, type, status);
        orderSagaEventPublisher.publish(event);
    }
}
```
- `order-orchestrator/src/main/java/com/example/orderorchestrator/application/service/ReserveExternalResourcesService.java`
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
- `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/webclient/dto/WebApiError.java`
```java
package com.example.orderorchestrator.adapter.out.webclient.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WebApiError {
    private final String code;
    private final String message;

    @JsonCreator
    public WebApiError(
            @JsonProperty("code") String code,
            @JsonProperty("message") String message
    ) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
```

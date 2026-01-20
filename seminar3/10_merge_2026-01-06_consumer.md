# 10. consumer -> main

## 시점
- 2026-01-06

## 비교 기준
- 직전 main 상태: `5a250f8d93a7476916f02f556c988f496c1d3ee0`
- 브랜치 tip: `5ebf028`

## 주요 변경(커밋 메시지 기반)
- ### Int Script 일부 정리 ###

## MSA + EDA + SAGA 관점 요약
- 컨슈머 처리 로직 추가/수정
- K8S/Kafka 배포 및 운영 스크립트

## 연결된 로직 흐름
- 이벤트 소비 -> 유스케이스/서비스 처리 -> 외부 서비스 호출/연동

## 핵심 로직 스니펫(머지 시점 기준)
- `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/adapter/in/kafka/OrderSagaEventConsumer.java`
```java
package com.example.ordersagaconsumer.adapter.in.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import com.example.ordersagaconsumer.application.port.in.ProcessOrderSagaEventUseCase;
import com.example.ordersagaconsumer.adapter.in.kafka.dto.OrderSagaEventPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderSagaEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessOrderSagaEventUseCase processOrderSagaEventUseCase;

    public OrderSagaEventConsumer(
            ObjectMapper objectMapper,
            ProcessOrderSagaEventUseCase processOrderSagaEventUseCase
    ) {
        this.objectMapper = objectMapper;
        this.processOrderSagaEventUseCase = processOrderSagaEventUseCase;
    }

    @KafkaListener(
            topics = "${order.saga.events.topic}",
            groupId = "${order.saga.events.consumer-group:order-saga-consumer}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        OrderSagaEventPayload payload = readPayload(record.value());
        if (payload == null) {
            return;
        }
        processOrderSagaEventUseCase.process(payload.orderId(), payload.status());
    }

    private OrderSagaEventPayload readPayload(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, OrderSagaEventPayload.class);
        } catch (Exception ex) {
            System.out.println("### Kafka payload parse failed ### : message=" + ex.getMessage()
                    + " payload=" + rawPayload);
            return null;
        }
    }
}
```

- `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/application/service/ProcessOrderSagaEventService.java`
```java
package com.example.ordersagaconsumer.application.service;

import com.example.ordersagaconsumer.application.port.in.ProcessOrderSagaEventUseCase;
import com.example.ordersagaconsumer.application.port.out.CouponServicePort;
import com.example.ordersagaconsumer.application.port.out.LoadOrderSagaPort;
import com.example.ordersagaconsumer.application.port.out.PointServicePort;
import com.example.ordersagaconsumer.application.port.out.UpdateOutboxMessagePort;
import com.example.ordersagaconsumer.domain.model.OrderSagaInfo;
import com.example.common.status.MSAStatus;
import com.example.common.status.OrderSagaStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProcessOrderSagaEventService implements ProcessOrderSagaEventUseCase {

    private final LoadOrderSagaPort loadOrderSagaPort;
    private final CouponServicePort couponServicePort;
    private final PointServicePort pointServicePort;
    private final UpdateOutboxMessagePort updateOutboxMessagePort;
    private final SagaStatusTransitionService sagaStatusTransitionService;

    public ProcessOrderSagaEventService(
            LoadOrderSagaPort loadOrderSagaPort,
            CouponServicePort couponServicePort,
            PointServicePort pointServicePort,
            UpdateOutboxMessagePort updateOutboxMessagePort,
            SagaStatusTransitionService sagaStatusTransitionService
    ) {
        this.loadOrderSagaPort = loadOrderSagaPort;
        this.couponServicePort = couponServicePort;
        this.pointServicePort = pointServicePort;
        this.updateOutboxMessagePort = updateOutboxMessagePort;
        this.sagaStatusTransitionService = sagaStatusTransitionService;
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
            sagaStatusTransitionService.markCompleted(orderId);
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
            sagaStatusTransitionService.markCompensated(orderId);
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
```
- `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/adapter/out/webclient/CouponServiceClient.java`
```java
package com.example.ordersagaconsumer.adapter.out.webclient;

import com.example.ordersagaconsumer.adapter.out.webclient.dto.CompensateCouponRequest;
import com.example.ordersagaconsumer.adapter.out.webclient.dto.ConfirmCouponRequest;
import com.example.ordersagaconsumer.adapter.out.webclient.support.ServiceClientSupport;
import com.example.ordersagaconsumer.application.port.out.CouponServicePort;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class CouponServiceClient extends ServiceClientSupport implements CouponServicePort {

    public CouponServiceClient(
            WebClient.Builder builder,
            @Value("${external.coupon.base-url}") String baseUrl,
            @Value("${external.client.timeout-seconds:3}") long timeoutSeconds,
            @Value("${external.client.retry-count:0}") int retryCount
    ) {
        super(builder, baseUrl, "Coupon", Duration.ofSeconds(timeoutSeconds), retryCount);
    }

    @Override
    public boolean confirm(String couponNumber, String orderId) {
        ConfirmCouponRequest request = new ConfirmCouponRequest(couponNumber, orderId);
        return post("/api/v1/coupons/confirm", request);
    }

    @Override
    public boolean compensate(String couponNumber, String orderId) {
        CompensateCouponRequest request = new CompensateCouponRequest(couponNumber, orderId);
        return post("/api/v1/coupons/compensate", request);
    }
}
```
- `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/adapter/out/webclient/PointServiceClient.java`
```java
package com.example.ordersagaconsumer.adapter.out.webclient;

import com.example.ordersagaconsumer.adapter.out.webclient.dto.CompensatePointRequest;
import com.example.ordersagaconsumer.adapter.out.webclient.dto.ConfirmPointRequest;
import com.example.ordersagaconsumer.adapter.out.webclient.support.ServiceClientSupport;
import com.example.ordersagaconsumer.application.port.out.PointServicePort;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PointServiceClient extends ServiceClientSupport implements PointServicePort {

    public PointServiceClient(
            WebClient.Builder builder,
            @Value("${external.point.base-url}") String baseUrl,
            @Value("${external.client.timeout-seconds:3}") long timeoutSeconds,
            @Value("${external.client.retry-count:0}") int retryCount
    ) {
        super(builder, baseUrl, "Point", Duration.ofSeconds(timeoutSeconds), retryCount);
    }

    @Override
    public boolean confirm(String pointNumber, String orderId) {
        ConfirmPointRequest request = new ConfirmPointRequest(pointNumber, orderId);
        return post("/api/v1/points/confirm", request);
    }

    @Override
    public boolean compensate(String pointNumber, String orderId) {
        CompensatePointRequest request = new CompensatePointRequest(pointNumber, orderId);
        return post("/api/v1/points/compensate", request);
    }
}
```
- `order-saga-consumer/src/main/resources/OSC_application.yaml`
```yaml
# src/main/resources/application.yml
spring:
  profiles:
    active: test  # file | h2

---
spring:
  config:
    activate:
      on-profile: test
  datasource:
    url: jdbc:mysql://localhost:3307/order_orchestrator_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
    username: order_orchestrator_user
    password: order_orchestrator_pw

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    defer-datasource-initialization: true

  kafka:
    bootstrap-servers: localhost:9094
    admin:
      auto-create: false
    consumer:
      group-id: order-saga-consumer-test
      auto-offset-reset: earliest
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

server:
  port: 8083

external:
  coupon:
    base-url: http://localhost:8081
  point:
    base-url: http://localhost:8082
order:
  saga:
    events:
      topic: order-saga-events-test
      consumer-group: order-saga-consumer-test

---
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:mysql://mysql.msa.svc.cluster.local:3306/order_orchestrator_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
    username: order_orchestrator_user
    password: ${ORDER_ORCH_DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    defer-datasource-initialization: true

  kafka:
    bootstrap-servers: kafka:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
  #    init:
  #      mode: embedded  #always | never | embedded

server:
  port: 8099

external:
  coupon:
    base-url: http://coupon-service.msa.svc.cluster.local:8081
  point:
    base-url: http://point-service.msa.svc.cluster.local:8082
order:
  saga:
    events:
      topic: order-saga-events
      consumer-group: order-saga-consumer-local

---
spring:
  config:
    activate:
      on-profile: k8s-local
  datasource:
    url: jdbc:mysql://localhost:3307/order_orchestrator_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
    username: order_orchestrator_user
    password: order_orchestrator_pw

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    defer-datasource-initialization: true

  kafka:
    bootstrap-servers: localhost:9094
    consumer:
      group-id: order-saga-consumer-local
      auto-offset-reset: earliest
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

server:
  port: 8094

external:
  coupon:
    base-url: http://localhost:8091
  point:
    base-url: http://localhost:8092
order:
  saga:
    events:
      topic: order-saga-events
```

## 쿠폰 confirm/compensate 처리 요약
- `CouponController`에서 `/confirm`, `/compensate` 요청을 받아 각각 `ConfirmCouponUseCase`, `CompensateCouponUseCase`로 위임한다.
- `ReserveCouponService`에서 confirm은 RESERVED -> USED로 전이하고, compensate는 RESERVED -> AVAILABLE로 되돌린다.
- 이미 USED 상태면 confirm은 멱등 처리(바로 return), compensate는 실패 처리로 막는다.

- `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`
```java
package com.example.couponservice.adapter.in.web;

import com.example.common.api.ApiResponse;
import com.example.couponservice.adapter.in.web.dto.request.CompensateCouponRequest;
import com.example.couponservice.adapter.in.web.dto.request.ConfirmCouponRequest;
import com.example.couponservice.adapter.in.web.dto.request.ReserveCouponRequest;
import com.example.couponservice.adapter.in.web.dto.response.CompensateCouponResponse;
import com.example.couponservice.adapter.in.web.dto.response.ConfirmCouponResponse;
import com.example.couponservice.adapter.in.web.dto.response.ReserveCouponResponse;
import com.example.couponservice.application.port.in.CompensateCouponUseCase;
import com.example.couponservice.application.port.in.ConfirmCouponUseCase;
import com.example.couponservice.application.port.in.ReserveCouponUseCase;
import com.example.couponservice.domain.model.status.CouponStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final ReserveCouponUseCase reserveCouponUseCase;
    private final ConfirmCouponUseCase confirmCouponUseCase;
    private final CompensateCouponUseCase compensateCouponUseCase;

    @PostMapping("/reserve")
    public ApiResponse<ReserveCouponResponse> reserveCoupon(@RequestBody ReserveCouponRequest request) {
        reserveCouponUseCase.reserve(request.couponNumber(), request.orderId());

        return ApiResponse.success(buildReserveResponse(request.couponNumber(), CouponStatus.RESERVED));
    }

    @PostMapping("/confirm")
    public ApiResponse<ConfirmCouponResponse> confirmCoupon(@RequestBody ConfirmCouponRequest request) {
        confirmCouponUseCase.confirm(request.couponNumber(), request.orderId());
        return ApiResponse.success(buildConfirmResponse(request.couponNumber(), CouponStatus.USED));
    }

    @PostMapping("/compensate")
    public ApiResponse<CompensateCouponResponse> compensateCoupon(@RequestBody CompensateCouponRequest request) {
        compensateCouponUseCase.compensateCoupon(request.couponNumber(), request.orderId());
        return ApiResponse.success(buildCompensateResponse(request.couponNumber(), CouponStatus.AVAILABLE));
    }

    private ReserveCouponResponse buildReserveResponse(String couponNumber, CouponStatus status) {
        return new ReserveCouponResponse(
                couponNumber,
                status.name()
        );
    }

    private ConfirmCouponResponse buildConfirmResponse(String couponNumber, CouponStatus status) {
        return new ConfirmCouponResponse(
                couponNumber,
                status.name()
        );
    }

    private CompensateCouponResponse buildCompensateResponse(String couponNumber, CouponStatus status) {
        return new CompensateCouponResponse(
                couponNumber,
                status.name()
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
import com.example.couponservice.application.port.out.SaveCouponPort;
import com.example.couponservice.domain.model.Coupon;
import com.example.couponservice.domain.model.status.CouponStatus;
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

    @Override
    public void reserve(String couponNumber, String orderId) {
        updateStatus(couponNumber, CouponStatus.RESERVED, this::validateReservable);
    }

    @Override
    public void confirm(String couponNumber, String orderId) {
        Coupon coupon = loadCouponPort.loadCoupon(couponNumber)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다: " + couponNumber));
        if (coupon.status() == CouponStatus.USED) {
            return;
        }
        validateConfirmable(coupon);

        Coupon updated = new Coupon(
                coupon.couponNumber(),
                CouponStatus.USED,
                coupon.issuedAt(),
                coupon.expiredAt()
        );
        saveCouponPort.save(updated);
    }

    @Override
    public void compensateCoupon(String couponNumber, String orderId) {
        Coupon coupon = loadCouponPort.loadCoupon(couponNumber)
                .orElse(null);
        if (coupon == null) {
            return;
        }
        if (coupon.status() == CouponStatus.USED) {
            throw new IllegalStateException("보상 불가능한 쿠폰입니다: " + coupon.couponNumber());
        }
        if (coupon.status() != CouponStatus.RESERVED) { //RESERVED 일 때만 보상 처리
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

    private void validateConfirmable(Coupon coupon) {
        if (coupon.status() != CouponStatus.RESERVED) {
            throw new IllegalStateException("확정 불가능한 쿠폰입니다: " + coupon.couponNumber());
        }
    }

}
```
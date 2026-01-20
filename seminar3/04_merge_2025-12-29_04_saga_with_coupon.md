# 04. 04_saga_with_coupon -> main

## 시점
- 2025-12-29

## 비교 기준
- 직전 main 상태: `e6603e9fa5a961bd5afff7b84a5f18d4142077af`
- 브랜치 tip: `b23abd0`

## 주요 변경(커밋 메시지 기반)
- mySQL 테스트 데이터 삭제 로직 개선
- Change mysql in K8s
- application.yaml 충돌 방지
- schema.sql 실행 이슈 관련 테스트 오류 수정
- 통합 테스트 개선
- Coupon-service 연계 통합 테스트

## MSA + EDA + SAGA 관점 요약
- 오케스트레이터 흐름 추가/수정
- 쿠폰 서비스 변경
- 공통 모듈 추가/수정
- DB 스키마/테스트 데이터 정리

## 연결된 로직 흐름
- API 요청 수신 -> 유스케이스/서비스 처리 -> 외부 서비스 호출/연동 -> 쿠폰 서비스 처리

## 핵심 로직 스니펫(머지 시점 기준)
- `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`
```java
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
```
- `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/webclient/CouponServiceClient.java`
```java
package com.example.orderorchestrator.adapter.out.webclient;

import com.example.common.api.ApiResponse;
import com.example.orderorchestrator.adapter.out.webclient.dto.ReserveCouponRequest;
import com.example.orderorchestrator.adapter.out.webclient.dto.ReserveCouponResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class CouponServiceClient {

    private final WebClient webClient;

    public CouponServiceClient(
            WebClient.Builder builder,
            @Value("${external.coupon.base-url}") String baseUrl
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    public Mono<ReserveCouponResponse> reserveCoupon(String couponNumber, String orderId) {
        ReserveCouponRequest request = new ReserveCouponRequest(couponNumber, orderId);

        return webClient.post()
                .uri("/api/v1/coupons/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<ReserveCouponResponse>>() {})
                .flatMap(response -> {
                    ReserveCouponResponse data = response.getData();
                    if (data == null) {
                        return Mono.error(new IllegalStateException("Reserve coupon response missing data"));
                    }
                    return Mono.just(data);
                });
    }
}
```
- `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/webclient/dto/ReserveCouponRequest.java`
```java
package com.example.orderorchestrator.adapter.out.webclient.dto;

public record ReserveCouponRequest(
        String couponNumber,
        String orderId
) {
}
```
- `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/webclient/dto/ReserveCouponResponse.java`
```java
package com.example.orderorchestrator.adapter.out.webclient.dto;

public record ReserveCouponResponse(
        String couponNumber,
        String status
) {
}
```
- `coupon-service/src/main/resources/coupon_application.yaml`
```yaml
# src/main/resources/application.yml
spring:
  profiles:
    active: test

---
spring:
  config:
    activate:
      on-profile: test

  datasource:
    url: jdbc:mysql://localhost:3307/coupon_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
    username: coupon_user
    password: coupon_pw

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    defer-datasource-initialization: true

  sql:
    init:
      mode: always
      schema-locations: classpath:coupon_schema.sql
  #    init:
  #      mode: embedded  #always | never | embedded

server:
  port: 8081

---
spring:
  config:
    activate:
      on-profile: dev

  datasource:
    url: jdbc:mysql://mysql.msa.svc.cluster.local:3306/coupon_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
    username: coupon_user
    password: ${COUPON_DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    defer-datasource-initialization: true

  sql:
    init:
      mode: always
      schema-locations: classpath:coupon_schema.sql
  #    init:
  #      mode: embedded  #always | never | embedded

server:
  port: 8081
```
- `coupon-service/src/main/resources/coupon_schema.sql`
```sql
CREATE TABLE IF NOT EXISTS coupon (
    coupon_number VARCHAR(255) PRIMARY KEY,
    status VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expired_at TIMESTAMP NOT NULL
);

truncate table coupon;

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'C-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);


INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);
```

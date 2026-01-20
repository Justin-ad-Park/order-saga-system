# 06. point_status -> main

## 시점
- 2025-12-31

## 비교 기준
- 직전 main 상태: `177839db7765679d5ed4f13b8c2a08db3228993d`
- 브랜치 tip: `fc9cbda`

## 주요 변경(커밋 메시지 기반)
- - 통합 테스트 리팩토링 - 쿠폰 실패, 포인트 성공 케이스 추가

## MSA + EDA + SAGA 관점 요약
- 오케스트레이터 흐름 추가/수정
- 쿠폰 서비스 변경
- 포인트 서비스 변경
- DB 스키마/테스트 데이터 정리

## 연결된 로직 흐름
- API 요청 수신 -> 쿠폰 서비스 처리 -> 포인트 서비스 처리

## 핵심 로직 스니펫(머지 시점 기준)
- `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`
```java
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
```
- `coupon-service/src/main/resources/coupon_schema.sql`
```sql
CREATE TABLE IF NOT EXISTS coupon (
                                      coupon_number VARCHAR(255) PRIMARY KEY,
    status VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expired_at TIMESTAMP NOT NULL
    );

TRUNCATE TABLE coupon;

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-BOTH-001',
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
           'CPN-BOTH-RESERVED-001',
           'RESERVED',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-ONLY-001',
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

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-INT-AVAILABLE-001',
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
           'CPN-INT-RESERVED-001',
           'RESERVED',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);
```
- `point-service/src/main/resources/point_schema.sql`
```sql
CREATE TABLE IF NOT EXISTS point (
                                     point_number VARCHAR(255) PRIMARY KEY,
    status VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expired_at TIMESTAMP NOT NULL
    );

TRUNCATE TABLE point;

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-BOTH-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-BOTH-AVAILABLE-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-ONLY-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'P-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-INT-AVAILABLE-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-INT-RESERVED-001',
           'RESERVED',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);
```
- `order-orchestrator/src/test/httprequest/01_orderOrchestratorK8sTest.http`
```
### 주문 생성 요청 (K8s Happy Path 예시)
POST http://localhost:8099/api/v1/orders
Content-Type: application/json
Accept: application/json

{
  "couponNumber": "CPN-BOTH-001",
  "pointNumber": "PNT-BOTH-001",
  "paymentNumber": "PAY-001",
  "paymentAmount": 35000,
  "orderItems": [
    {
      "itemNumber": "ITEM-001",
      "quantity": 2
    },
    {
      "itemNumber": "ITEM-002",
      "quantity": 1
    }
  ]
}

###

# 주문 생성 요청 (쿠폰 예약 불가 + 포인트 예약 가능)
POST http://localhost:8099/api/v1/orders
Content-Type: application/json
Accept: application/json

{
  "couponNumber": "CPN-BOTH-RESERVED-001",
  "pointNumber": "PNT-BOTH-AVAILABLE-001",
  "paymentNumber": "PAY-001",
  "paymentAmount": 35000,
  "orderItems": [
    {
      "itemNumber": "ITEM-001",
      "quantity": 2
    },
    {
      "itemNumber": "ITEM-002",
      "quantity": 1
    }
  ]
}

###

# 주문 생성 요청 (쿠폰만)
POST http://localhost:8099/api/v1/orders
Content-Type: application/json
Accept: application/json

{
  "couponNumber": "CPN-ONLY-001",
  "paymentNumber": "PAY-001",
  "paymentAmount": 35000,
  "orderItems": [
    {
      "itemNumber": "ITEM-001",
      "quantity": 2
    },
    {
      "itemNumber": "ITEM-002",
      "quantity": 1
    }
  ]
}

###

# 주문 생성 요청 (포인트만)
POST http://localhost:8099/api/v1/orders
Content-Type: application/json
Accept: application/json

{
  "pointNumber": "PNT-ONLY-001",
  "paymentNumber": "PAY-001",
  "paymentAmount": 35000,
  "orderItems": [
    {
      "itemNumber": "ITEM-001",
      "quantity": 2
    },
    {
      "itemNumber": "ITEM-002",
      "quantity": 1
    }
  ]
}

###

# 주문 생성 요청 (쿠폰/포인트 없음)
POST http://localhost:8099/api/v1/orders
Content-Type: application/json
Accept: application/json

{
  "paymentNumber": "PAY-001",
  "paymentAmount": 35000,
  "orderItems": [
    {
      "itemNumber": "ITEM-001",
      "quantity": 2
    },
    {
      "itemNumber": "ITEM-002",
      "quantity": 1
    }
  ]
}

###

# (선택) 서버 헬스 체크
GET http://localhost:8099/actuator/health
Accept: application/json
```
- `order-orchestrator/src/test/httprequest/01_orderOrchestratorTest.http`
```
### 루트에서 MSA 서비스 실행 ###
# bin/run_local_msa.sh
#
# 프로세스 종료는
# bin/stop_local_msa.sh

### 주문 생성 요청 (Happy Path 예시)
POST http://localhost:8080/api/v1/orders
Content-Type: application/json
Accept: application/json

{
  "couponNumber": "CPN-BOTH-001",
  "pointNumber": "PNT-BOTH-001",
  "paymentNumber": "PAY-001",
  "paymentAmount": 35000,
  "orderItems": [
    {
      "itemNumber": "ITEM-001",
      "quantity": 2
    },
    {
      "itemNumber": "ITEM-002",
      "quantity": 1
    }
  ]
}

###

# 주문 생성 요청 (쿠폰 예약 불가 + 포인트 예약 가능)
POST http://localhost:8080/api/v1/orders
Content-Type: application/json
Accept: application/json

{
  "couponNumber": "CPN-BOTH-RESERVED-001",
  "pointNumber": "PNT-BOTH-AVAILABLE-001",
  "paymentNumber": "PAY-001",
  "paymentAmount": 35000,
  "orderItems": [
    {
      "itemNumber": "ITEM-001",
      "quantity": 2
    },
    {
      "itemNumber": "ITEM-002",
      "quantity": 1
    }
  ]
}

###

# 주문 생성 요청 (쿠폰만)
POST http://localhost:8080/api/v1/orders
Content-Type: application/json
Accept: application/json

{
  "couponNumber": "CPN-ONLY-001",
  "paymentNumber": "PAY-001",
  "paymentAmount": 35000,
  "orderItems": [
    {
      "itemNumber": "ITEM-001",
      "quantity": 2
    },
    {
      "itemNumber": "ITEM-002",
      "quantity": 1
    }
  ]
}

###

# 주문 생성 요청 (포인트만)
POST http://localhost:8080/api/v1/orders
Content-Type: application/json
Accept: application/json

{
  "pointNumber": "PNT-ONLY-001",
  "paymentNumber": "PAY-001",
  "paymentAmount": 35000,
  "orderItems": [
    {
      "itemNumber": "ITEM-001",
      "quantity": 2
    },
    {
      "itemNumber": "ITEM-002",
      "quantity": 1
    }
  ]
}

###

# 주문 생성 요청 (쿠폰/포인트 없음)
POST http://localhost:8080/api/v1/orders
Content-Type: application/json
Accept: application/json

{
  "paymentNumber": "PAY-001",
  "paymentAmount": 35000,
  "orderItems": [
    {
      "itemNumber": "ITEM-001",
      "quantity": 2
    },
    {
      "itemNumber": "ITEM-002",
      "quantity": 1
    }
  ]
}

###

# (선택) 서버 헬스 체크
GET http://localhost:8080/actuator/health
Accept: application/json

###

# H2 콘솔 호출 (HTML 응답, 브라우저에서 여는 게 확인하기 편함)
GET http://localhost:8080/h2-console


// # H2 DB 데이터 확인
SELECT * FROM ORDER_ITEM ;
SELECT * FROM ORDER_SAGA ;
SELECT * FROM OUTBOX_MESSAGE ;
```
- `order-orchestrator/src/test/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationIntegrationTest.java`
```java
// src/test/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationIntegrationTest.java
package com.example.orderorchestrator.adapter.in.web;

import com.example.orderorchestrator.adapter.in.web.dto.response.CreateOrderResponse;
import com.example.couponservice.CouponServiceApplication;
import com.example.pointservice.PointServiceApplication;
import com.example.orderorchestrator.adapter.out.persistence.jpa.OrderSagaJpaRepository;
import com.example.orderorchestrator.adapter.out.persistence.jpa.OutboxMessageJpaRepository;
import com.example.orderorchestrator.adapter.out.persistence.jpa.entity.OrderSagaJpaEntity;
import com.example.orderorchestrator.adapter.out.persistence.jpa.entity.OutboxMessageJpaEntity;
import com.example.orderorchestrator.domain.model.status.MSAStatus;
import com.example.orderorchestrator.domain.model.status.OrderSagaStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *  CLI Test 방법
 *  ./gradlew :order-orchestrator:test --tests "OrderOrchestrationIntegrationTest"
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.name=orderOS_application")
@ActiveProfiles("test")
@Sql(
        scripts = "/orderOS_cleanup.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
)
@Transactional
class OrderOrchestrationIntegrationTest {

    private static ConfigurableApplicationContext couponContext;
    private static int couponPort;
    private static ConfigurableApplicationContext pointContext;
    private static int pointPort;


    @AfterAll
    static void stopMSAService() {
        if (couponContext != null) {
            couponContext.close();
        }
        if (pointContext != null) {
            pointContext.close();
        }
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        if (couponContext == null) {
            ServiceContext context = startService(
                    CouponServiceApplication.class,
                    "coupon_application",
                    "coupon_schema.sql",
                    8081,
                    "coupon"
            );
            couponContext = context.context();
            couponPort = context.port();
        }

        registry.add("external.coupon.base-url", () -> "http://localhost:" + couponPort);

        if (pointContext == null) {
            ServiceContext context = startService(
                    PointServiceApplication.class,
                    "point_application",
                    "point_schema.sql",
                    8082,
                    "point"
            );
            pointContext = context.context();
            pointPort = context.port();
        }

        registry.add("external.point.base-url", () -> "http://localhost:" + pointPort);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderSagaJpaRepository orderSagaJpaRepository;

    @Autowired
    private OutboxMessageJpaRepository outboxMessageJpaRepository;

    //@AfterEach
    void tearDown() {
        outboxMessageJpaRepository.deleteAll();
        orderSagaJpaRepository.deleteAll();
    }

    // 쿠폰과 포인트 모두 예약 가능한 경우
    @Test
    void createOrder_withCouponAndPoint_shouldPersistOrderSaga_and_OutboxMessage() {
        // given: 주문 생성 요청 바디
        Map<String, Object> requestBody = Map.of(
                "couponNumber", "CPN-BOTH-001",
                "pointNumber", "PNT-BOTH-001",
                "paymentNumber", "PAY-001",
                "paymentAmount", 35000L,
                "orderItems", List.of(
                        Map.of("itemNumber", "ITEM-001", "quantity", 2),
                        Map.of("itemNumber", "ITEM-002", "quantity", 1)
                )
        );

        assertOrderCreated(requestBody, MSAStatus.Reserved, MSAStatus.Reserved);
    }

    // 쿠폰만 사용하는 경우
    @Test
    void createOrder_withCouponOnly_shouldPersistOrderSaga_and_OutboxMessage() {
        Map<String, Object> requestBody = Map.of(
                "couponNumber", "CPN-ONLY-001",
                "paymentNumber", "PAY-001",
                "paymentAmount", 35000L,
                "orderItems", List.of(
                        Map.of("itemNumber", "ITEM-001", "quantity", 2),
                        Map.of("itemNumber", "ITEM-002", "quantity", 1)
                )
        );

        assertOrderCreated(requestBody, MSAStatus.Reserved, MSAStatus.NotUsed);
    }

    // 포인트만 사용하는 경우
    @Test
    void createOrder_withPointOnly_shouldPersistOrderSaga_and_OutboxMessage() {
        Map<String, Object> requestBody = Map.of(
                "pointNumber", "PNT-ONLY-001",
                "paymentNumber", "PAY-001",
                "paymentAmount", 35000L,
                "orderItems", List.of(
                        Map.of("itemNumber", "ITEM-001", "quantity", 2),
                        Map.of("itemNumber", "ITEM-002", "quantity", 1)
                )
        );

        assertOrderCreated(requestBody, MSAStatus.NotUsed, MSAStatus.Reserved);
    }

    // 쿠폰/포인트 없이 주문하는 경우
    @Test
    void createOrder_withoutCouponAndPoint_shouldPersistOrderSaga_and_OutboxMessage() {
        Map<String, Object> requestBody = Map.of(
                "paymentNumber", "PAY-001",
                "paymentAmount", 35000L,
                "orderItems", List.of(
                        Map.of("itemNumber", "ITEM-001", "quantity", 2),
                        Map.of("itemNumber", "ITEM-002", "quantity", 1)
                )
        );

        assertOrderCreated(requestBody, MSAStatus.NotUsed, MSAStatus.NotUsed);
    }

    // 쿠폰은 이미 예약되어 실패하고, 포인트는 예약 가능한 경우
    @Test
    void createOrder_withReservedCouponAndAvailablePoint_shouldMarkCouponFailedAndPointReserved() {
        Map<String, Object> requestBody = Map.of(
                "couponNumber", "CPN-BOTH-RESERVED-001",
                "pointNumber", "PNT-BOTH-AVAILABLE-001",
                "paymentNumber", "PAY-001",
                "paymentAmount", 35000L,
                "orderItems", List.of(
                        Map.of("itemNumber", "ITEM-001", "quantity", 2),
                        Map.of("itemNumber", "ITEM-002", "quantity", 1)
                )
        );

        assertOrderCreatedWithExternalFailure(requestBody, MSAStatus.Failed, MSAStatus.Reserved);
    }

    private void assertOrderCreated(Map<String, Object> requestBody, MSAStatus expectedCouponStatus, MSAStatus expectedPointStatus) {
        HttpEntity<Map<String, Object>> httpEntity = buildHttpEntity(requestBody);

        // when: /api/v1/orders 호출
        ResponseEntity<CreateOrderResponse> response = restTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                httpEntity,
                CreateOrderResponse.class
        );

        // then: HTTP 응답 검증
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        CreateOrderResponse body = response.getBody();
        String orderId = body.orderId();
        String sagaId = body.sagaId();
        String status = body.status();

        assertThat(orderId).isNotBlank();
        assertThat(sagaId).isNotBlank();
        assertThat(status).isEqualTo(OrderSagaStatus.InProgress.name());

        // 그리고 H2 DB에 order_saga, outbox_message 가 잘 들어갔는지 확인

        // 1) order_saga 테이블
        Optional<OrderSagaJpaEntity> sagaOpt = orderSagaJpaRepository.findByOrderId(orderId);
        assertThat(sagaOpt).isPresent();

        OrderSagaJpaEntity sagaEntity = sagaOpt.get();
        assertOrderSaga(sagaEntity, orderId, sagaId);
        assertOutbox(orderId, expectedCouponStatus, expectedPointStatus, true);
    }

    private void assertOrderCreatedWithExternalFailure(Map<String, Object> requestBody, MSAStatus expectedCouponStatus, MSAStatus expectedPointStatus) {
        HttpEntity<Map<String, Object>> httpEntity = buildHttpEntity(requestBody);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                httpEntity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        OrderSagaJpaEntity sagaEntity = findLatestSaga();
        String orderId = sagaEntity.getOrderId();

        assertOrderSaga(sagaEntity, orderId, sagaEntity.getSagaId());
        assertOutbox(orderId, expectedCouponStatus, expectedPointStatus, false);
    }

    private HttpEntity<Map<String, Object>> buildHttpEntity(Map<String, Object> requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(requestBody, headers);
    }

    private OrderSagaJpaEntity findLatestSaga() {
        List<OrderSagaJpaEntity> sagas = orderSagaJpaRepository.findAll();
        assertThat(sagas).isNotEmpty();
        return sagas.stream()
                .max(Comparator.comparing(OrderSagaJpaEntity::getId))
                .orElseThrow();
    }

    private void assertOrderSaga(OrderSagaJpaEntity sagaEntity, String orderId, String sagaId) {
        assertThat(orderId).isNotBlank();
        assertThat(sagaId).isNotBlank();
        assertThat(sagaEntity.getOrderId()).isEqualTo(orderId);
        assertThat(sagaEntity.getSagaId()).isEqualTo(sagaId);
        assertThat(sagaEntity.getStatus()).isEqualTo(OrderSagaStatus.InProgress);
        assertThat(sagaEntity.getItems()).hasSize(2);
    }

    private void assertOutbox(
            String orderId,
            MSAStatus expectedCouponStatus,
            MSAStatus expectedPointStatus,
            boolean expectPayload
    ) {
        Optional<OutboxMessageJpaEntity> outboxOpt = outboxMessageJpaRepository.findByOrderId(orderId);
        assertThat(outboxOpt).isPresent();

        OutboxMessageJpaEntity outboxEntity = outboxOpt.get();
        assertThat(outboxEntity.getOrderId()).isEqualTo(orderId);
        assertThat(outboxEntity.getCouponStatus()).isEqualTo(expectedCouponStatus);
        assertThat(outboxEntity.getPointStatus()).isEqualTo(expectedPointStatus);
        assertThat(outboxEntity.getOrderStatus()).isEqualTo(MSAStatus.InProgress);
        assertThat(outboxEntity.getPaymentStatus()).isEqualTo(MSAStatus.InProgress);
        assertThat(outboxEntity.getSagaStatus()).isEqualTo(OrderSagaStatus.InProgress);
        if (expectPayload) {
            assertThat(outboxEntity.getPayload()).isEqualTo("{}");
        }
    }

    private static ServiceContext startService(
            Class<?> applicationClass,
            String configName,
            String schemaFileName,
            int fallbackPort,
            String serviceName
    ) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(applicationClass)
                .properties(
                        "server.port=0",
                        "spring.profiles.active=test",
                        "spring.config.name=" + configName
                )
                .run();

        int port;
        if (context instanceof ServletWebServerApplicationContext servletContext) {
            port = servletContext.getWebServer().getPort();
        } else {
            port = context.getEnvironment().getProperty("local.server.port", Integer.class, fallbackPort);
        }

        System.out.println("\n==========================");
        System.out.println(serviceName.toUpperCase() + "_PORT: " + port);
        System.out.println(serviceName + " spring.datasource.url = " +
                context.getEnvironment().getProperty("spring.datasource.url"));

        System.out.println(serviceName + " spring.sql.init.mode = " +
                context.getEnvironment().getProperty("spring.sql.init.mode"));

        System.out.println(serviceName + " spring.sql.init.schema-locations = " +
                context.getEnvironment().getProperty("spring.sql.init.schema-locations"));

        var schemaResource = context.getResource("classpath:/" + schemaFileName);
        System.out.println(schemaFileName + " exists? " + schemaResource.exists() + ", url=" + schemaResource);
        System.out.println("==========================");

        return new ServiceContext(context, port);
    }

    private static final class ServiceContext {
        private final ConfigurableApplicationContext context;
        private final int port;

        private ServiceContext(ConfigurableApplicationContext context, int port) {
            this.context = context;
            this.port = port;
        }

        private ConfigurableApplicationContext context() {
            return context;
        }

        private int port() {
            return port;
        }
    }
}
```

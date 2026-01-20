# 02. 02_order_repository -> main

## 시점
- 2025-12-15

## 비교 기준
- 직전 main 상태: `868aa6f4ce5cf5384bd03a3c4495b6178f5d142a`
- 브랜치 tip: `e37883c`

## 주요 변경(커밋 메시지 기반)
- ### Common 모듈 추가 ######################

## MSA + EDA + SAGA 관점 요약
- 오케스트레이터 흐름 추가/수정
- 공통 모듈 추가/수정

## 연결된 로직 흐름
- 유스케이스/서비스 처리

## 핵심 로직 스니펫(머지 시점 기준)
- `order-orchestrator/src/main/java/com/example/orderorchestrator/application/service/CreateOrderService.java`
```java
package com.example.orderorchestrator.application.service;

import com.example.orderorchestrator.application.port.in.command.CreateOrderCommand;
import com.example.orderorchestrator.application.port.in.result.CreateOrderResult;
import com.example.orderorchestrator.application.port.in.CreateOrderUseCase;
import com.example.orderorchestrator.application.port.out.SaveOrderSagaPort;
import com.example.orderorchestrator.application.port.out.SaveOutboxMessagePort;
import com.example.orderorchestrator.domain.model.OrderItem;
import com.example.orderorchestrator.domain.model.OrderSaga;
import com.example.orderorchestrator.domain.model.status.OrderSagaStatus;
import com.example.orderorchestrator.domain.outbox.OutboxMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.example.common.api.uuid.UUIDGenerator.createUuid;

@Service
@Transactional
public class CreateOrderService implements CreateOrderUseCase {

    private final SaveOrderSagaPort saveOrderSagaPort;
    private final SaveOutboxMessagePort saveOutboxMessagePort;

    public CreateOrderService(
            SaveOrderSagaPort saveOrderSagaPort,
            SaveOutboxMessagePort saveOutboxMessagePort
    ) {
        this.saveOrderSagaPort = saveOrderSagaPort;
        this.saveOutboxMessagePort = saveOutboxMessagePort;
    }

    @Override
    public CreateOrderResult createOrder(CreateOrderCommand command) {
        // 1) 주문ID / SagaID 생성 (임시: UUID 기반)
        String orderId = "ORD-" + createUuid();
        String sagaId = "SAGA-" + createUuid();

        // 2) Command → 도메인 OrderItem 리스트 변환
        List<OrderItem> items = command.orderItems().stream()
                .map(i -> new OrderItem(i.itemNumber(), i.quantity()))
                .collect(Collectors.toList());

        // 3) OrderSaga 엔티티 생성 (초기 상태: InProgress)
        OrderSaga saga = OrderSaga.create(
                orderId,
                sagaId,
                command.couponNumber(),
                command.paymentNumber(),
                command.paymentAmount(),
                items,
                OrderSagaStatus.InProgress   // ✅ 변경된 enum 사용
        );

        // 4) Saga 저장
        OrderSaga savedSaga = saveOrderSagaPort.save(saga);

        // 5) Outbox 메시지 생성 (payload는 우선 빈 JSON으로 두고, 나중에 스키마 설계)
        OutboxMessage message = OutboxMessage.initial(
                savedSaga.orderId(),   // ✅ 새 구조: orderId만 전달
                "{}"                   // payload (TODO: 실제 JSON으로 교체)
        );

        // 6) Outbox 저장
        saveOutboxMessagePort.save(message);

        // 7) 결과 반환
        return CreateOrderResult.of(
                savedSaga.orderId(),
                savedSaga.sagaId(),
                savedSaga.status().name()  // OrderSagaStatus → String
        );
    }


}
```
- `common/src/main/java/com/example/common/api/ApiError.java`
```java
package com.example.common.api;

// 공통 에러 DTO (web 계층)
public class ApiError {
    private final String code;
    private final String message;
    private ApiError(String code, String message) { this.code = code; this.message = message; }
    public String getCode() { return code; }
    public String getMessage() { return message; }

    public static ApiError of(String code, String message) {
        return new ApiError(code, message);
    }
}
```
- `common/src/main/java/com/example/common/api/ApiResponse.java`
```java
package com.example.common.api;

// 서버 측에서 사용할 공통 응답 DTO
public class ApiResponse<T> {
    private final boolean success; // 성공 여부
    private final T data;          // 성공 시 반환할 데이터
    private final ApiError error;  // 실패 시 반환할 에러 정보 (기존 ApiError 재사용)

    // 성공 응답
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    // 실패 응답 (ApiError를 인자로 받음)
    public static <T> ApiResponse<T> failure(ApiError error) {
        return new ApiResponse<>(false, null, error);
    }

    private ApiResponse(boolean success, T data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public boolean isSuccess() { return success; }
    public T getData() { return data; }
    public ApiError getError() { return error; }
}
```
- `common/src/main/java/com/example/common/api/uuid/UUIDGenerator.java`
```java
package com.example.common.api.uuid;

import java.util.UUID;
public class UUIDGenerator {
    public static UUID createUuid() {
        return UUID.randomUUID();
    }
}
```
- `order-orchestrator/build.gradle`
```
plugins {
    id 'org.springframework.boot'
    id 'io.spring.dependency-management' version '1.1.5'
    id 'java'
}

dependencies {
    // Web API (기존 ver08과 동일하게 MVC 기반으로 시작)
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // 오케스트레이터용 DB
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-jdbc'
    runtimeOnly 'com.h2database:h2'

    // mybatis
    implementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.3'


    // 헥사고날 구조 검증용 ArchUnit
    testImplementation 'com.tngtech.archunit:archunit-junit5:1.3.0'

    // Spring Boot 테스트 (TDD)
    testImplementation 'org.springframework.boot:spring-boot-starter-test'

    // (ver08에서 사용하던 UUID 관련 라이브러리 – 그대로 가져옴, 필요 없으면 제거해도 됨)
    testImplementation 'com.fasterxml.uuid:java-uuid-generator:5.0.0'   // UUIDv7
    testImplementation 'com.github.f4b6a3:ulid-creator:5.2.0'           // ULID
    testImplementation 'com.github.ksuid:ksuid:1.1.2'                   // KSUID

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    testCompileOnly 'org.projectlombok:lombok'
    testAnnotationProcessor 'org.projectlombok:lombok'

    implementation project(':common')
}
```
- `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/GlobalExceptionHandler.java`
```java
package com.example.orderorchestrator.adapter.in.web;

import com.example.common.api.ApiError;
import com.example.common.api.ApiResponse;
import com.example.orderorchestrator.domain.exception.NotFoundException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountNotFoundException;

// 전역 예외 처리기 (ApiResponse 패턴 적용 - 분리된 핸들러)
@ControllerAdvice
public class GlobalExceptionHandler {

    // 1. AccountNotFoundException (404 Not Found 관련) 처리
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(NotFoundException ex) {
        return responseEntityWithHttpStatus(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    // 2. IllegalArgumentException (400 Bad Request 관련) 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(IllegalArgumentException ex) {
        return responseEntityWithHttpStatus(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }

    // 3. IllegalStateException (409 Conflict 관련) 처리
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleConflict(IllegalStateException ex) {
        return responseEntityWithHttpStatus(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage());
    }


     /* 최종 fallback: 잡히지 않은 모든 Exception (500 Internal Server Error) 처리 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleServerError(Exception ex) {
        return responseEntityWithHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR", "Internal Server Error occurred.");
    }

    private static ResponseEntity<ApiResponse<Object>> responseEntityWithHttpStatus(HttpStatus notFound, String NOT_FOUND, String ex) {
        return ResponseEntity
                .status(notFound)      // 🔹 404
                .body(ApiResponse.failure(ApiError.of(NOT_FOUND, ex)));
    }

}
```

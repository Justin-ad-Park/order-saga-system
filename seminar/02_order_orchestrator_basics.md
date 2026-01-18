# 02. 주문 오케스트레이터 기본 구조

## 목표
- 주문 오케스트레이터의 핵심 유스케이스와 책임 분리를 이해한다.

## 스토리라인
- 주문 생성 흐름이 하나의 메서드에 몰리면서 복잡도가 급증.
- 책임을 분리해 테스트와 가독성을 확보.

## 관련 커밋
- `82e897a`, `478c995`, `73b1f75`, `b304524`, `3eb2580`

## 커밋 변경 요약
| 커밋 | 주요 변경 요약                                                                                                              | 체크아웃 |
| --- |-----------------------------------------------------------------------------------------------------------------------| --- |
| `82e897a` | 주문 오케스트레이터의 기본 골격만 완성. Persistent 개발 안됨                                                                               | `git checkout 82e897a` |
| `478c995` | UUID Generator 분리                                                                                                     | `git checkout 478c995` |
| `73b1f75` | add JPA Library and application.yaml setting                                                                          | `git checkout 73b1f75` |
| `b304524` | JPA 기본 구조 완성                                                                                                          | `git checkout b304524` |
| `3eb2580` | OrderOrchestrationController.java에서 createOrder가 <br/>“커맨드 생성 + 유즈케이스 호출 + 외부 호출”을 한 메서드에 모두 포함. <br/>책임 분리를 통해 테스트/가독성 개선 | `git checkout 3eb2580` |

## 핵심 개념
- 유스케이스 분리와 역할 나누기
- JPA 도입과 스키마 적용

## 기술/기능/프로세스
- 기술: Spring Boot, JPA/Hibernate, REST Controller, TestRestTemplate
- 기능: 주문 생성, 유스케이스 분리, 외부 호출 오케스트레이션 기초
- MSA: 오케스트레이터 책임 분리
- EDA: outbox/event 발행을 위한 구조 준비
## 데모/실습
- 통합 테스트 흐름 확인: `order-orchestrator/src/test/java/.../OrderOrchestrationIntegrationTest.java`
- 데이터베이스 설정 확인: `order-orchestrator/src/main/resources/orderOS_application.yaml`

## 코드 발췌 및 설명
- `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`: 주문 생성 → 외부 예약 → saga 상태 업데이트/이벤트 발행 흐름
```java
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
```
- 왜 필요한가: 주문 생성 이후 외부 예약과 사가 상태 전이가 어디서 묶이는지 보여줘, 오케스트레이션 책임을 이해시키기 좋다.

## 커밋 상세
### 82e897a 주문 오케스트레이터의 기본 골격만 완성. Persistent 개발 안됨
- 변경 요약: 주문 오케스트레이터의 기본 골격만 완성. Persistent 개발 안됨
- 핵심 로직: 모듈/의존성 구성 변경
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `order-orchestrator/build.gradle`, `order-orchestrator/src/main/java/com/example/orderorchestrator/OrderOrchestratorApplication.java`
- 코드 발췌: `order-orchestrator/build.gradle`
```diff
+
+    compileOnly 'org.projectlombok:lombok'
+    annotationProcessor 'org.projectlombok:lombok'
+
+    testCompileOnly 'org.projectlombok:lombok'
+    testAnnotationProcessor 'org.projectlombok:lombok'
```
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/OrderOrchestratorApplication.java`
```diff
+package com.example.orderorchestrator;
+
+import org.springframework.boot.SpringApplication;
+import org.springframework.boot.autoconfigure.SpringBootApplication;
+
+
+@SpringBootApplication
+public class OrderOrchestratorApplication {
```

### 478c995 UUID Generator 분리
- 변경 요약: UUID Generator 분리
- 핵심 로직: 식별자 생성 로직 분리
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `order-orchestrator/src/main/java/com/example/orderorchestrator/application/service/CreateOrderService.java`, `order-orchestrator/src/main/java/com/example/orderorchestrator/application/service/uuid/UUIDGenerator.java`
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/application/service/CreateOrderService.java`
```diff
+import static com.example.orderorchestrator.application.service.uuid.UUIDGenerator.createUuid;
+
+        String orderId = "ORD-" + createUuid();
+        String sagaId = "SAGA-" + createUuid();
+
```
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/application/service/uuid/UUIDGenerator.java`
```diff
+package com.example.orderorchestrator.application.service.uuid;
+
+import java.util.UUID;
+public class UUIDGenerator {
+    public static UUID createUuid() {
+        return UUID.randomUUID();
+    }
+}
```

### 73b1f75 add JPA Library and application.yaml setting
- 변경 요약: add JPA Library and application.yaml setting
- 핵심 로직: 모듈/의존성 구성 변경
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `order-orchestrator/build.gradle`, `order-orchestrator/src/main/resources/application.yaml`
- 코드 발췌: `order-orchestrator/build.gradle`
```diff
+    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
```
- 코드 발췌: `order-orchestrator/src/main/resources/application.yaml`
```diff
+    url: jdbc:h2:mem:orderorchestrator;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MYSQL
+    hibernate:
+      ddl-auto: update
+    show-sql: true
+#    init:
+#      mode: embedded  #always | never | embedded
+#mybatis:
+#  mapper-locations: classpath*:mappers/**/*.xml     # ★ XML 위치
```

### b304524 JPA 기본 구조 완성
- 변경 요약: JPA 기본 구조 완성
- 핵심 로직: 모듈/의존성 구성 변경
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `order-orchestrator/build.gradle`, `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/OrderSagaPersistenceAdapter.java`
- 코드 발췌: `order-orchestrator/build.gradle`
```diff
+    id 'io.spring.dependency-management' version '1.1.5'
+    // 오케스트레이터용 DB
+    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
```
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/OrderSagaPersistenceAdapter.java`
```diff
+// src/main/java/com/example/orderorchestrator/adapter/out/persistence/OrderSagaPersistenceAdapter.java
+package com.example.orderorchestrator.adapter.out.persistence;
+
+import com.example.orderorchestrator.adapter.out.persistence.jpa.entity.OrderItemJpaEntity;
+import com.example.orderorchestrator.adapter.out.persistence.jpa.entity.OrderSagaJpaEntity;
+import com.example.orderorchestrator.application.port.out.SaveOrderSagaPort;
+import com.example.orderorchestrator.adapter.out.persistence.jpa.OrderSagaJpaRepository;
+import com.example.orderorchestrator.domain.model.OrderItem;
```

### 3eb2580 OrderOrchestrationController.java에서 createOrder가 “커맨드 생성 + 유즈케이스 호출 + 외부 호출”을 한 메서드에 모두 포함. 책임 분리를 통해 테스트/가독성 개선
- 변경 요약: OrderOrchestrationController.java에서 createOrder가 “커맨드 생성 + 유즈케이스 호출 + 외부 호출”을 한 메서드에 모두 포함. 책임 분리를 통해 테스트/가독성 개선
- 핵심 로직: 테스트 케이스 확장
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`
```diff
+        return reserveExternalResources(request, result)
+                .thenReturn(ResponseEntity.ok(mapToResponse(result)));
+
+    private CreateOrderResponse mapToResponse(CreateOrderResult result) {
+        return CreateOrderResponse.of(
+                result.orderId(),
+                result.sagaId(),
+                result.status()
```

### 82e897a 주문 오케스트레이터의 기본 골격만 완성. Persistent 개발 안됨
- 변경 요약: 주문 오케스트레이터의 기본 골격만 완성. Persistent 개발 안됨
- 핵심 로직: 모듈/의존성 구성 변경
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `order-orchestrator/build.gradle`, `order-orchestrator/src/main/java/com/example/orderorchestrator/OrderOrchestratorApplication.java`
- 코드 발췌: `order-orchestrator/build.gradle`
```diff
+
+    compileOnly 'org.projectlombok:lombok'
+    annotationProcessor 'org.projectlombok:lombok'
+
+    testCompileOnly 'org.projectlombok:lombok'
+    testAnnotationProcessor 'org.projectlombok:lombok'
```
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/OrderOrchestratorApplication.java`
```diff
+package com.example.orderorchestrator;
+
+import org.springframework.boot.SpringApplication;
+import org.springframework.boot.autoconfigure.SpringBootApplication;
+
+
+@SpringBootApplication
+public class OrderOrchestratorApplication {
```

### 478c995 UUID Generator 분리
- 변경 요약: UUID Generator 분리
- 핵심 로직: 식별자 생성 로직 분리
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `order-orchestrator/src/main/java/com/example/orderorchestrator/application/service/CreateOrderService.java`, `order-orchestrator/src/main/java/com/example/orderorchestrator/application/service/uuid/UUIDGenerator.java`
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/application/service/CreateOrderService.java`
```diff
+import static com.example.orderorchestrator.application.service.uuid.UUIDGenerator.createUuid;
+
+        String orderId = "ORD-" + createUuid();
+        String sagaId = "SAGA-" + createUuid();
+
```
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/application/service/uuid/UUIDGenerator.java`
```diff
+package com.example.orderorchestrator.application.service.uuid;
+
+import java.util.UUID;
+public class UUIDGenerator {
+    public static UUID createUuid() {
+        return UUID.randomUUID();
+    }
+}
```

### 73b1f75 add JPA Library and application.yaml setting
- 변경 요약: add JPA Library and application.yaml setting
- 핵심 로직: 모듈/의존성 구성 변경
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `order-orchestrator/build.gradle`, `order-orchestrator/src/main/resources/application.yaml`
- 코드 발췌: `order-orchestrator/build.gradle`
```diff
+    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
```
- 코드 발췌: `order-orchestrator/src/main/resources/application.yaml`
```diff
+    url: jdbc:h2:mem:orderorchestrator;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MYSQL
+    hibernate:
+      ddl-auto: update
+    show-sql: true
+#    init:
+#      mode: embedded  #always | never | embedded
+#mybatis:
+#  mapper-locations: classpath*:mappers/**/*.xml     # ★ XML 위치
```

### b304524 JPA 기본 구조 완성
- 변경 요약: JPA 기본 구조 완성
- 핵심 로직: 모듈/의존성 구성 변경
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `order-orchestrator/build.gradle`, `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/OrderSagaPersistenceAdapter.java`
- 코드 발췌: `order-orchestrator/build.gradle`
```diff
+    id 'io.spring.dependency-management' version '1.1.5'
+    // 오케스트레이터용 DB
+    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
```
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/OrderSagaPersistenceAdapter.java`
```diff
+// src/main/java/com/example/orderorchestrator/adapter/out/persistence/OrderSagaPersistenceAdapter.java
+package com.example.orderorchestrator.adapter.out.persistence;
+
+import com.example.orderorchestrator.adapter.out.persistence.jpa.entity.OrderItemJpaEntity;
+import com.example.orderorchestrator.adapter.out.persistence.jpa.entity.OrderSagaJpaEntity;
+import com.example.orderorchestrator.application.port.out.SaveOrderSagaPort;
+import com.example.orderorchestrator.adapter.out.persistence.jpa.OrderSagaJpaRepository;
+import com.example.orderorchestrator.domain.model.OrderItem;
```

### 3eb2580 OrderOrchestrationController.java에서 createOrder가 “커맨드 생성 + 유즈케이스 호출 + 외부 호출”을 한 메서드에 모두 포함. 책임 분리를 통해 테스트/가독성 개선
- 변경 요약: OrderOrchestrationController.java에서 createOrder가 “커맨드 생성 + 유즈케이스 호출 + 외부 호출”을 한 메서드에 모두 포함. 책임 분리를 통해 테스트/가독성 개선
- 핵심 로직: 테스트 케이스 확장
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`
```diff
+        return reserveExternalResources(request, result)
+                .thenReturn(ResponseEntity.ok(mapToResponse(result)));
+
+    private CreateOrderResponse mapToResponse(CreateOrderResult result) {
+        return CreateOrderResponse.of(
+                result.orderId(),
+                result.sagaId(),
+                result.status()
```

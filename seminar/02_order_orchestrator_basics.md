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

## 커밋 상세
### 82e897a 주문 오케스트레이터의 기본 골격만 완성. Persistent 개발 안됨
- 주요 변경: 주문 오케스트레이터의 기본 골격만 완성. Persistent 개발 안됨
- 핵심 코드: `order-orchestrator/src/main/java/com/example/orderorchestrator/domain/outbox/OutboxMessage.java`
```java
public class OutboxMessage {
//--- 생략 ...
    private final String payload;               // 메시지 payload(JSON)

    private MSAStatus couponStatus;
    private MSAStatus orderStatus;
    private MSAStatus paymentStatus;

    private OrderSagaStatus sagaStatus;

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OutboxMessage(
            String orderId,
            String payload,
            MSAStatus couponStatus,
            MSAStatus orderStatus,
            MSAStatus paymentStatus,
            OrderSagaStatus sagaStatus,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.orderId = orderId;
        this.payload = payload;
        this.couponStatus = couponStatus;
        this.orderStatus = orderStatus;
        this.paymentStatus = paymentStatus;
        this.sagaStatus = sagaStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
//--- 생략 ...
}
```
- 설명: Outbox에 이벤트를 적재해 DB 트랜잭션과 이벤트 발행을 분리한다.
- 연결 포인트: 여기서 생성된 `orderId`/`sagaId`가 컨트롤러의 `createOrder`에서 외부 예약 호출(쿠폰/포인트)과 연결되어 사가 흐름이 이어진다.

### 478c995 UUID Generator 분리
- 주요 변경: UUID Generator 분리
- 핵심 코드: `order-orchestrator/src/main/java/com/example/orderorchestrator/application/service/CreateOrderService.java`
```java
public class CreateOrderService implements CreateOrderUseCase {
//--- 생략 ...
    public CreateOrderResult createOrder(CreateOrderCommand command) {
        String orderId = "ORD-" + createUuid();
        String sagaId = "SAGA-" + createUuid();

        List<OrderItem> items = command.orderItems().stream()
                .map(i -> new OrderItem(i.itemNumber(), i.quantity()))
                .collect(Collectors.toList());

        OrderSaga saga = OrderSaga.create(
                orderId,
                sagaId,
                command.couponNumber(),
                command.paymentNumber(),
                command.paymentAmount(),
                items,
                OrderSagaStatus.InProgress
        );

        OrderSaga savedSaga = saveOrderSagaPort.save(saga);

        OutboxMessage message = OutboxMessage.initial(
                savedSaga.orderId(),
                "{}"
        );

        saveOutboxMessagePort.save(message);

        return CreateOrderResult.of(
                savedSaga.orderId(),
                savedSaga.sagaId(),
                savedSaga.status().name()
        );
    }
//--- 생략 ...
}
```
- 설명: Outbox에 이벤트를 적재해 DB 트랜잭션과 이벤트 발행을 분리한다.

### 73b1f75 add JPA Library and application.yaml setting
- 주요 변경: add JPA Library and application.yaml setting
- 핵심 코드: `order-orchestrator/src/main/resources/application.yaml`
```yaml
//--- 생략 ...
    activate:
      on-profile: h2
  datasource:
    url: jdbc:h2:mem:orderorchestrator;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MYSQL
    #url: "jdbc:h2:file:${user.home}/test/account-db;MODE=LEGACY"
    driver-class-name: org.h2.Driver
    username: sa
    password:
  sql:
//--- 생략 ...
```
- 설명: 서비스 구성값을 분리해 환경별 MSA 연동을 명확히 한다.

### b304524 JPA 기본 구조 완성
- 주요 변경: JPA 기본 구조 완성
- 핵심 코드: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/jpa/entity/OutboxMessageJpaEntity.java`
```java
public class OutboxMessageJpaEntity {
//--- 생략 ...
    protected OutboxMessageJpaEntity() {
    }
//--- 생략 ...
}
```
- 설명: Outbox에 이벤트를 적재해 DB 트랜잭션과 이벤트 발행을 분리한다.

### 3eb2580 OrderOrchestrationController.java에서 createOrder가 “커맨드 생성 + 유즈케이스 호출 + 외부 호출”을 한 메서드에 모두 포함. 책임 분리를 통해 테스트/가독성 개선
- 주요 변경: OrderOrchestrationController.java에서 createOrder가 “커맨드 생성 + 유즈케이스 호출 + 외부 호출”을 한 메서드에 모두 포함. 책임 분리를 통해 테스트/가독성 개선
- 핵심 코드: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`
```java
public class OrderOrchestrationController {
//--- 생략 ...
    @PostMapping
    public Mono<ResponseEntity<CreateOrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        CreateOrderCommand command = mapToCommand(request);
        CreateOrderResult result = createOrderUseCase.createOrder(command);

        return reserveExternalResources(request, result)
                .thenReturn(ResponseEntity.ok(mapToResponse(result)));
    }
//--- 생략 ...
}
```
- 설명: Saga 상태 전이를 명시해 흐름의 단계가 코드로 드러나게 한다.

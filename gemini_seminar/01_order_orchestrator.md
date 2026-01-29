# Chapter 1: Order Orchestrator와 Hexagonal Architecture

## 1. 개요: MSA 여정의 시작점

본 챕터에서는 MSA 기반 주문 시스템 개발 여정의 첫 걸음인 `order-orchestrator` 서비스의 초기 구조를 살펴봅니다. `order-orchestrator`는 전체 주문 프로세스의 흐름을 조율하고 관리하는 중앙 허브 역할을 담당하게 됩니다. 특히, 유연하고 테스트하기 쉬운 시스템 설계를 위해 **Hexagonal Architecture (Ports and Adapters)** 원칙을 어떻게 적용했는지 중점적으로 알아봅니다.

### 핵심 학습 목표
*   Hexagonal Architecture의 기본 개념과 MSA 설계에 미치는 영향을 이해합니다.
*   `order-orchestrator`의 초기 구성 요소와 역할을 파악합니다.
*   ArchUnit을 활용하여 아키텍처 규칙을 코드로 검증하는 방법을 학습합니다.

## 2. Hexagonal Architecture (Ports and Adapters) 상세 이해

Hexagonal Architecture는 애플리케이션의 핵심 비즈니스 로직(Domain)을 외부 기술 및 인프라로부터 격리하여 변경에 유연하게 대응하고 테스트 용이성을 높이는 설계 패턴입니다.

*   **Domain (Core):** 애플리케이션의 심장부로, 순수한 비즈니스 규칙과 프로세스를 담고 있습니다. 외부 세계(DB, UI, 외부 API 등)에 대한 어떠한 직접적인 의존성도 가지지 않습니다.
*   **Ports:** 도메인 계층이 외부와 상호작용하기 위한 **인터페이스**입니다.
    *   **Input Ports (Use Cases):** 도메인 기능을 외부에서 호출하기 위한 인터페이스. "어떤 기능을 수행할 것인가?"를 정의합니다. (예: `CreateOrderUseCase`)
    *   **Output Ports (Repositories, External Services):** 도메인이 외부 서비스를 호출하기 위한 인터페이스. "외부에서 어떤 정보가 필요한가, 어떤 동작을 기대하는가?"를 정의합니다. (예: `SaveOrderSagaPort`, `ReserveCouponPort`)
*   **Adapters:** Ports 인터페이스의 **실제 구현체**입니다. 외부 기술과의 연동을 담당하며, 도메인에 영향을 주지 않고 교체 가능합니다.
    *   **Driving Adapters (Input Adapters):** 사용자 인터페이스(REST API, CLI 등)나 스케줄러와 같이 Input Port를 호출하는 어댑터. (예: `OrderOrchestrationController`)
    *   **Driven Adapters (Output Adapters):** Output Port를 구현하여 데이터베이스(JPA Repository), 메시지 브로커(Kafka Producer), 외부 서비스 호출(WebClient)과 같은 실제 인프라와의 통신을 처리하는 어댑터. (예: `OrderSagaPersistenceAdapter`, `CouponServiceClient`)

**왜 Hexagonal Architecture인가?**
*   **유연성:** 인프라 기술이 변경되어도 핵심 비즈니스 로직은 그대로 유지될 수 있습니다 (예: JPA -> R2DBC로 변경).
*   **테스트 용이성:** 도메인 로직을 단위 테스트할 때 Mock 객체를 활용하여 외부 의존성 없이 순수하게 비즈니스 규칙만을 검증할 수 있습니다.
*   **재사용성:** 도메인 로직은 어떤 유형의 애플리케이션(Web, CLI, Batch)에서도 재사용 가능합니다.

## 3. `order-orchestrator`의 초기 Git 이력과 변화

`order-orchestrator` 서비스는 Git 커밋 히스토리를 통해 Hexagonal Architecture 원칙을 적용하며 초기 골격을 구축했습니다. 다음은 주요 Git 커밋입니다.

| 커밋 ID | 날짜 | 주요 변경 요약 |
|---|---|---|
| `a080f1d` | 2025-12-11 | `order-orchestrator` 프로젝트 시작 (기본 골격) |
| `82e897a` | 2025-12-12 | 주문 오케스트레이터의 기본 골격 완성. Persistent 개발 전 |
| `73b1f75` | 2025-12-12 | JPA 라이브러리 및 `application.yaml` 설정 추가 |
| `b304524` | 2025-12-12 | JPA 기본 구조 완성 (엔티티, 레포지토리 등) |
| `e37883c` | 2025-12-15 | `common` 모듈 추가 (공통 코드 재사용성 확보) |
| `868aa6f` | 2025-12-15 | ArchUnit 검증 테스트 추가 (아키텍처 규칙 강제) |
| `478c995` | 2025-12-15 | UUID Generator 분리 (공통 유틸리티) |

**(실습 가이드: Git 커밋 확인)**
1.  프로젝트 루트에서 `git log --oneline -n 7` 명령어를 실행하여 위 커밋들을 직접 확인해 보세요.
2.  `git checkout 868aa6f` 명령어로 해당 커밋 시점으로 이동하여 당시의 코드 상태를 확인해 볼 수 있습니다. (확인 후 `git checkout main` 등으로 돌아오세요.)

## 4. 핵심 비즈니스 로직 및 아키텍처 구현

### 4.1. Controller (Driving Adapter) - `OrderOrchestrationController`

클라이언트의 주문 요청(HTTP POST `/api/v1/orders`)을 받아 `CreateOrderUseCase` (Input Port)를 호출하는 Driving Adapter 역할을 합니다. 초기에는 주문 생성 로직만 담당합니다.

**`order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`**
```java
// ... imports ...
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderOrchestrationController {

    private final CreateOrderUseCase createOrderUseCase; // Input Port

    @PostMapping
    public Mono<ResponseEntity<CreateOrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        CreateOrderCommand command = mapToCommand(request);
        CreateOrderResult result = createOrderUseCase.createOrder(command); // Input Port 호출

        // 초기 구현에서는 단순히 결과만 반환
        return Mono.just(ResponseEntity.ok(mapToResponse(result)));
    }
    // ... (mapToCommand, mapToResponse 등 헬퍼 메서드 생략)
}
```

### 4.2. Service (Application Layer) - `CreateOrderService`

`CreateOrderUseCase` (Input Port) 인터페이스를 구현하며, 실제 주문 생성 및 Saga 데이터 초기화 비즈니스 로직을 담당합니다. Hexagonal Architecture의 Application Layer에 해당합니다. `SaveOrderSagaPort`와 `SaveOutboxMessagePort` (Output Ports)를 통해 인프라 계층과 상호작용합니다.

**`order-orchestrator/src/main/java/com/example/orderorchestrator/application/service/CreateOrderService.java`**
```java
// ... imports ...
@Service
@Transactional // 트랜잭션 관리
public class CreateOrderService implements CreateOrderUseCase { // Input Port 구현체

    private final SaveOrderSagaPort saveOrderSagaPort;       // Output Port
    private final SaveOutboxMessagePort saveOutboxMessagePort; // Output Port

    public CreateOrderService(
            SaveOrderSagaPort saveOrderSagaPort,
            SaveOutboxMessagePort saveOutboxMessagePort
    ) {
        this.saveOrderSagaPort = saveOrderSagaPort;
        this.saveOutboxMessagePort = saveOutboxMessagePort;
    }

    @Override
    public CreateOrderResult createOrder(CreateOrderCommand command) {
        String orderId = "ORD-" + createUuid(); // UUID 생성 유틸리티 활용
        String sagaId = "SAGA-" + createUuid();

        List<OrderItem> items = command.orderItems().stream()
                .map(i -> new OrderItem(i.itemNumber(), i.quantity()))
                .collect(Collectors.toList());

        // OrderSaga 엔티티 생성 (초기 상태: InProgress)
        OrderSaga saga = OrderSaga.create(
                orderId, sagaId, command.couponNumber(), command.pointNumber(),
                command.paymentNumber(), command.paymentAmount(), items, OrderSagaStatus.InProgress
        );

        OrderSaga savedSaga = saveOrderSagaPort.save(saga); // Output Port를 통해 Saga 저장

        // Outbox 메시지 생성 (Saga 상태 추적 용도)
        MSAStatus couponStatus = resolveUsageStatus(command.couponNumber());
        MSAStatus pointStatus = resolveUsageStatus(command.pointNumber());
        OutboxMessage message = OutboxMessage.initial(
                savedSaga.orderId(), "{}", couponStatus, pointStatus
        );

        saveOutboxMessagePort.save(message); // Output Port를 통해 Outbox 메시지 저장

        return CreateOrderResult.of(
                savedSaga.orderId(), savedSaga.sagaId(), savedSaga.status().name()
        );
    }
    // ... (createUuid, resolveUsageStatus 등 헬퍼 메서드 생략)
}
```
**`createUuid()` 유틸리티:** `common` 모듈에 위치한 `UUIDGenerator`는 `ORD-`나 `SAGA-` 접두사와 함께 고유 ID를 생성하는 유틸리티입니다. 이처럼 공통 로직은 `common` 모듈로 분리하여 재사용성을 높입니다.

### 4.3. ArchUnit으로 아키텍처 규칙 강제하기

`Hexagonal Architecture`와 같은 아키텍처 규칙은 코드가 복잡해지고 개발자가 많아질수록 지키기 어렵습니다. **ArchUnit**은 코드 레벨에서 아키텍처 규칙을 검증하고 강제하는 데 사용됩니다.

먼저, `common` 모듈에 모든 서비스가 공통으로 사용할 ArchUnit 테스트 템플릿을 정의합니다. 이 템플릿은 '도메인 계층은 다른 계층에 의존할 수 없다'와 같은 핵심 아키텍처 규칙을 포함합니다.

**`common/src/testFixtures/java/com/example/common/archunit/HexagonalArchitectureTestTemplate.java`**
```java
// ... imports ...
public abstract class HexagonalArchitectureTestTemplate {

    protected abstract String basePackage(); // 각 서비스의 기본 패키지 지정

    private HexagonalArchitectureRules rules() {
        return HexagonalArchitectureRules.getInstance(basePackage());
    }

    // 예시: 도메인 계층은 어떤 프레임워크에도 의존해서는 안 된다는 규칙
    @ArchTest
    void domain_should_not_depend_on_any_framework(JavaClasses importedClasses) {
        rules().domainShouldNotDependOnAnyFramework().check(importedClasses);
    }

    // 예시: 애플리케이션 계층은 어댑터에 직접 의존해서는 안 된다는 규칙
    @ArchTest
    void application_should_not_depend_on_adapters(JavaClasses importedClasses) {
        rules().applicationShouldNotDependOnAdapters().check(importedClasses);
    }
    // ... (다른 아키텍처 규칙들)
}
```

이제 `order-orchestrator` 서비스에서는 이 템플릿을 상속받고 자신의 기본 패키지(`com.example.orderorchestrator`)만 지정해주면, `Hexagonal Architecture`의 공통 규칙들을 자동으로 검증할 수 있습니다.

**`order-orchestrator/src/test/java/com/example/orderorchestrator/archunit/ArchitectureTest4OrderOrchestrator.java`**
```java
// ... imports ...
@AnalyzeClasses(
        packages = ArchitectureTest4OrderOrchestrator.BASE_PACKAGE,
        importOptions = { ImportOption.DoNotIncludeTests.class }
)
public class ArchitectureTest4OrderOrchestrator extends HexagonalArchitectureTestTemplate {

    static final String BASE_PACKAGE = "com.example.orderorchestrator";

    @Override
    protected String basePackage() {
        return BASE_PACKAGE;
    }
}
```

**(실습 가이드: ArchUnit 테스트 실행)**
1.  프로젝트 루트에서 `./gradlew :order-orchestrator:test` 명령어를 실행해 보세요.
2.  테스트 결과 중 `ArchitectureTest4OrderOrchestrator` 부분이 성공했는지 확인해 보세요. 이는 `order-orchestrator`가 정의된 아키텍처 규칙을 잘 준수하고 있다는 의미입니다.

---

## 5. 실습 체크포인트

*   **프로젝트 구조 확인:**
    *   `settings.gradle` 파일을 열어 `order-orchestrator`, `coupon-service`, `point-service`, `common`, `order-saga-consumer` 모듈이 포함되어 있는지 확인합니다.
    *   프로젝트 루트에서 `./gradlew projects` 명령어를 실행하여 Gradle이 인식하는 모듈 목록을 확인해 봅니다.
*   **ArchUnit 테스트 실행:**
    *   `./gradlew :order-orchestrator:test` 명령어를 실행하여 ArchUnit 테스트를 포함한 `order-orchestrator`의 모든 테스트를 실행하고 성공하는지 확인합니다.

---
이 단계를 통해 우리는 견고하고 확장 가능한 아키텍처의 기반 위에서 `order-orchestrator`라는 첫 번째 마이크로서비스를 구축했습니다. 다음 챕터에서는 이 구조 위에 어떻게 새로운 마이크로서비스(`coupon-service`)를 추가하고 연동하는지 알아보겠습니다.
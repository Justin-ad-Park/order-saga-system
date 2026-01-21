# Chapter 1: Order Orchestrator와 Hexagonal Architecture

본 챕터에서는 `order-saga-system` 프로젝트의 첫 시작인 `order-orchestrator`의 기본 구조와 핵심 설계 사상인 **Hexagonal Architecture**에 대해 알아봅니다.

## 1. 프로젝트의 시작

이 프로젝트는 MSA와 EDA 환경에서 주문 시스템을 구축하는 여정의 첫 걸음입니다. 모든 것은 단일 서비스, `order-orchestrator`에서 시작합니다. 이 서비스는 전체 주문 프로세스의 흐름을 조율하고 관리하는 중앙 허브 역할을 담당하게 됩니다.

## 2. Hexagonal Architecture (Ports and Adapters)

프로젝트 초기부터 **Hexagonal Architecture**를 도입하여 비즈니스 로직(Domain)과 외부 기술(Infrastructure)을 분리하는 데 중점을 두었습니다.

*   **Domain (Core):** 순수한 비즈니스 규칙과 프로세스를 담고 있으며, 외부 세계에 대한 의존성이 전혀 없습니다.
*   **Ports:** 도메인 계층과의 상호작용을 위한 인터페이스입니다. Use Case(Input Port)와 Repository(Output Port)가 여기에 해당됩니다.
*   **Adapters:** 외부 세계와 상호작용하는 실제 구현체입니다.
    *   **Input/Web Adapter:** 외부 요청(예: REST API)을 받아 Use Case를 호출합니다.
    *   **Output/Persistence Adapter:** Port(Repository 인터페이스)를 구현하여 데이터베이스와 통신합니다.

이 구조는 유연하고 테스트하기 쉬운 시스템을 만듭니다. 예를 들어, 데이터베이스를 H2에서 MySQL로 변경하더라도 비즈니스 로직은 전혀 영향을 받지 않습니다.

## 3. 주요 Git 이력

아래 커밋들은 `order-orchestrator`의 초기 골격을 만들고 Hexagonal Architecture를 적용하는 과정을 보여줍니다.

```
* 868aa6f | 2025-12-12 | Archunit 검증 테스트 추가
* b304524 | 2025-12-12 | JPA 기본 구조 완성
* 73b1f75 | 2025-12-12 | add JPA Library and application.yaml setting
* 82e897a | 2025-12-12 | 주문 오케스트레이터의 기본 골격만 완성. Persistent 개발 안됨
* a080f1d | 2025-12-11 | order start
```

## 핵심 비즈니스 로직

### 1) Controller (Input Adapter) - `OrderOrchestrationController`

클라이언트의 주문 요청을 받아 `CreateOrderUseCase`를 호출하는 Input Adapter 역할을 합니다.
초기에는 `CreateOrderUseCase.createOrder()` 호출까지만 담당하며, 이후 외부 서비스 호출 및 이벤트 발행 로직이 추가됩니다.

**`order-orchestrator/.../in/web/OrderOrchestrationController.java`**
```java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderOrchestrationController {

    private final CreateOrderUseCase createOrderUseCase;
    // ... (다른 필드 및 생성자 생략)

    @PostMapping
    public Mono<ResponseEntity<CreateOrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        // 1. 요청 Command 로 매핑
        CreateOrderCommand command = mapToCommand(request);
        // 2. UseCase 호출하여 주문 생성
        CreateOrderResult result = createOrderUseCase.createOrder(command);

        // ... (이후 외부 서비스 호출 및 이벤트 발행 로직은 다음 챕터에서 추가)

        // 초기 구현에서는 단순히 결과만 반환
        return Mono.just(ResponseEntity.ok(mapToResponse(result)));
    }
    // ... (헬퍼 메서드 생략)
}
```

### 2) Service (Application Layer) - `CreateOrderService`

`CreateOrderUseCase` 인터페이스를 구현하며, 실제 주문 생성 및 Saga 데이터 초기화 비즈니스 로직을 담당합니다. Hexagonal Architecture의 Application Layer에 해당합니다.

**`order-orchestrator/.../application/service/CreateOrderService.java`**
```java
@Service
@Transactional
public class CreateOrderService implements CreateOrderUseCase {

    private final SaveOrderSagaPort saveOrderSagaPort;
    private final SaveOutboxMessagePort saveOutboxMessagePort;

    // ... constructor ...

    @Override
    public CreateOrderResult createOrder(CreateOrderCommand command) {
        // 1) 주문ID / SagaID 생성
        String orderId = "ORD-" + createUuid();
        String sagaId = "SAGA-" + createUuid();

        // 2) Command → 도메인 OrderItem 리스트 변환
        List<OrderItem> items = command.orderItems().stream()
                .map(i -> new OrderItem(i.itemNumber(), i.quantity()))
                .collect(Collectors.toList());

        // 3) OrderSaga 엔티티 생성 (초기 상태: InProgress)
        OrderSaga saga = OrderSaga.create(
                orderId, sagaId, command.couponNumber(), command.pointNumber(),
                command.paymentNumber(), command.paymentAmount(), items, OrderSagaStatus.InProgress
        );

        // 4) Saga 저장 (Output Port 사용)
        OrderSaga savedSaga = saveOrderSagaPort.save(saga);

        // 5) Outbox 메시지 생성 (Saga 상태 추적 용도)
        MSAStatus couponStatus = resolveUsageStatus(command.couponNumber());
        MSAStatus pointStatus = resolveUsageStatus(command.pointNumber());
        OutboxMessage message = OutboxMessage.initial(
                savedSaga.orderId(), "{}", couponStatus, pointStatus
        );

        // 6) Outbox 저장 (Output Port 사용)
        saveOutboxMessagePort.save(message);

        // 7) 결과 반환
        return CreateOrderResult.of(
                savedSaga.orderId(), savedSaga.sagaId(), savedSaga.status().name()
        );
    }
    // ... (헬퍼 메서드 생략)
}
```

## 4. 핵심 코드 스니펫

### ArchUnit으로 아키텍처 규칙 강제하기

`868aa6f` 커밋에서 추가된 **ArchUnit** 테스트는 코드 레벨에서 Hexagonal Architecture의 의존성 규칙을 강제합니다.

먼저 `common` 모듈에 모든 서비스가 공통으로 사용할 테스트 템플릿을 만듭니다. 이 템플릿은 'domain은 다른 계층에 의존할 수 없다' 와 같은 핵심 규칙을 정의합니다.

**`common/src/testFixtures/java/com/example/common/archunit/HexagonalArchitectureTestTemplate.java`**
```java
public abstract class HexagonalArchitectureTestTemplate {

    protected abstract String basePackage();

    private HexagonalArchitectureRules rules() {
        return HexagonalArchitectureRules.getInstance(basePackage());
    }

    @ArchTest
    void domain_should_not_depend_on_any_framework(JavaClasses importedClasses) {
        rules().domainShouldNotDependOnAnyFramework().check(importedClasses);
    }

    @ArchTest
    void application_should_not_depend_on_adapters(JavaClasses importedClasses) {
        rules().applicationShouldNotDependOnAdapters().check(importedClasses);
    }

    // ... and other rules
}
```

그 다음, `order-orchestrator` 서비스에서는 이 템플릿을 상속받아 자신의 패키지 정보만 지정해주면, 공통 규칙을 모두 적용하여 테스트할 수 있습니다.

**`order-orchestrator/.../archunit/ArchitectureTest4OrderOrchestrator.java`**
```java
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
이처럼 아키텍처 규칙을 코드로 검증함으로써, 프로젝트가 커지고 여러 개발자가 참여하더라도 설계의 일관성을 유지할 수 있습니다.

---
이 단계를 통해 우리는 견고하고 확장 가능한 아키텍처의 기반 위에서 `order-orchestrator`라는 첫 번째 마이크로서비스를 구축했습니다. 다음 챕터에서는 이 구조 위에 어떻게 새로운 마이크로서비스(`coupon-service`)를 추가하고 연동하는지 알아보겠습니다.
# 03. 공통 모듈과 아키텍처 테스트

## 목표
- 공통 모듈의 역할과 ArchUnit을 통한 구조 검증을 이해한다.

## 스토리라인
- 모듈 간 의존성이 무너지기 시작하면서 구조 검증 도구가 필요해짐.

## 관련 커밋
- `e37883c`, `868aa6f`, `1475eba`, `6e8df39`

## 커밋 변경 요약
| 커밋 | 주요 변경 요약 | 체크아웃 |
| --- | --- | --- |
| `e37883c` | ### Common 모듈 추가 ###################### | `git checkout e37883c` |
| `868aa6f` | Archunit 검증 테스트 추가 | `git checkout 868aa6f` |
| `1475eba` | ArchitectureUnit 테스트 추가 | `git checkout 1475eba` |
| `6e8df39` | Archunit 중복제거 리팩토링 | `git checkout 6e8df39` |

## 핵심 개념
- common 모듈로 상태 모델/공통 DTO 공유
- ArchUnit으로 계층 규칙 강제

## 기술/기능/프로세스
- 기술: ArchUnit, common 모듈
- 기능: 계층/의존성 규칙 검증
- MSA: 모듈 경계 강제
- EDA: 이후 단계에서 이벤트 흐름 검증에 확장 가능
## 데모/실습
- ArchUnit 테스트 확인: `order-orchestrator/src/test/java/.../ArchitectureTest4OrderOrchestrator.java`

## 커밋 상세
### e37883c ### Common 모듈 추가 ######################
- 주요 변경: ### Common 모듈 추가 ######################
- 핵심 코드: `order-orchestrator/src/test/java/com/example/orderorchestrator/archunit/ArchitectureTest.java`
```java
public class ArchitectureTest {
//--- 생략 ...
                            PORT_OUT,                 // port.out (반드시 이를 통해 도메인/외부와 연결)
                            DOMAIN,                   // 도메인 모델/상태
                            "java..",
                            "jakarta..",
                            "javax..",
                            "org.springframework..",
                            "lombok.."                // 필요하다면
                    );

    private static final String DOMAIN_MODEL    = "..domain..model..";
    private static final String DOMAIN_STATUS   = "..domain..model..status..";

    // =====================================================
    // 6. JPA 엔티티는 도메인 엔티티를 참조하면 안 된다
    // =====================================================
    /**
     * JPA 엔티티가 Domain Model(엔터티/값객체 등) 에 직접 의존하지 않도록 강제하는 규칙.
     *
     * 헥사고날 아키텍처(Ports & Adapters)에서는 Persistence Layer(JPA)가
     * 도메인의 내부 모델(domain.model.*)을 직접 참조하는 것이 금지된다.
     * 그래야 도메인 로직이 인프라(JPA)에 오염되지 않고,
     * 또한 persistence 구현체 교체 시(예: JPA → R2DBC → Mongo) 도메인이 안전하게 유지된다.
     *
     * 단, domain.model.status.* 패키지의 Enum(MSAStatus, OrderSagaStatus)은 예외로 허용한다.
     * 이 상태 값들은 도메인의 공통 언어(Ubiquitous Language)이자 스키마와 1:1 매핑되는 값으로서,
     * JPA 엔티티에서 상태 필드로 참조하는 것이 구조적으로 자연스럽기 때문이다.
     *
     * 요약:
     *   - 금지: JPA → domain.model.*, domain.model.saga.*, domain.model.order.* 등
     *   - 허용: JPA → domain.model.status.* (MSAStatus, OrderSagaStatus)
     *
//--- 생략 ...
}
```
- 설명: Saga 상태 전이를 명시해 흐름의 단계가 코드로 드러나게 한다.

### 868aa6f Archunit 검증 테스트 추가
- 주요 변경: Archunit 검증 테스트 추가
- 핵심 코드: `order-orchestrator/src/test/java/com/example/orderorchestrator/archunit/ArchitectureTest.java`
```java
public class ArchitectureTest {
//--- 생략 ...
                            PORT_OUT,                 // port.out (반드시 이를 통해 도메인/외부와 연결)
                            DOMAIN,                   // 도메인 모델/상태
                            "java..",
                            "jakarta..",
                            "javax..",
                            "org.springframework..",
                            "lombok.."                // 필요하다면
                    );

    private static final String DOMAIN_MODEL    = "..domain..model..";
    private static final String DOMAIN_STATUS   = "..domain..model..status..";

    // =====================================================
    // 6. JPA 엔티티는 도메인 엔티티를 참조하면 안 된다
    // =====================================================
    /**
     * JPA 엔티티가 Domain Model(엔터티/값객체 등) 에 직접 의존하지 않도록 강제하는 규칙.
     *
     * 헥사고날 아키텍처(Ports & Adapters)에서는 Persistence Layer(JPA)가
     * 도메인의 내부 모델(domain.model.*)을 직접 참조하는 것이 금지된다.
     * 그래야 도메인 로직이 인프라(JPA)에 오염되지 않고,
     * 또한 persistence 구현체 교체 시(예: JPA → R2DBC → Mongo) 도메인이 안전하게 유지된다.
     *
     * 단, domain.model.status.* 패키지의 Enum(MSAStatus, OrderSagaStatus)은 예외로 허용한다.
     * 이 상태 값들은 도메인의 공통 언어(Ubiquitous Language)이자 스키마와 1:1 매핑되는 값으로서,
     * JPA 엔티티에서 상태 필드로 참조하는 것이 구조적으로 자연스럽기 때문이다.
     *
     * 요약:
     *   - 금지: JPA → domain.model.*, domain.model.saga.*, domain.model.order.* 등
     *   - 허용: JPA → domain.model.status.* (MSAStatus, OrderSagaStatus)
     *
//--- 생략 ...
}
```
- 설명: Saga 상태 전이를 명시해 흐름의 단계가 코드로 드러나게 한다.

### 1475eba ArchitectureUnit 테스트 추가
- 주요 변경: ArchitectureUnit 테스트 추가
- 핵심 코드: `coupon-service/src/test/java/com/example/couponservice/archunit/ArchitectureTest4CouponSercice.java`
```java
public class ArchitectureTest4CouponSercice {
//--- 생략 ...
                            PORT_OUT,                 // port.out (반드시 이를 통해 도메인/외부와 연결)
                            DOMAIN,                   // 도메인 모델/상태
                            "java..",
                            "jakarta..",
                            "javax..",
                            "org.springframework..",
                            "lombok.."                // 필요하다면
                    );

    private static final String DOMAIN_MODEL    = "..domain..model..";
    private static final String DOMAIN_STATUS   = "..domain..model..status..";

    // =====================================================
    // 6. JPA 엔티티는 도메인 엔티티를 참조하면 안 된다
    // =====================================================
    /**
     * JPA 엔티티가 Domain Model(엔터티/값객체 등) 에 직접 의존하지 않도록 강제하는 규칙.
     *
     * 헥사고날 아키텍처(Ports & Adapters)에서는 Persistence Layer(JPA)가
     * 도메인의 내부 모델(domain.model.*)을 직접 참조하는 것이 금지된다.
     * 그래야 도메인 로직이 인프라(JPA)에 오염되지 않고,
     * 또한 persistence 구현체 교체 시(예: JPA → R2DBC → Mongo) 도메인이 안전하게 유지된다.
     *
     * 단, domain.model.status.* 패키지의 Enum(MSAStatus, OrderSagaStatus)은 예외로 허용한다.
     * 이 상태 값들은 도메인의 공통 언어(Ubiquitous Language)이자 스키마와 1:1 매핑되는 값으로서,
     * JPA 엔티티에서 상태 필드로 참조하는 것이 구조적으로 자연스럽기 때문이다.
     *
     * 요약:
     *   - 금지: JPA → domain.model.*, domain.model.saga.*, domain.model.order.* 등
     *   - 허용: JPA → domain.model.status.* (MSAStatus, OrderSagaStatus)
     *
//--- 생략 ...
}
```
- 설명: Saga 상태 전이를 명시해 흐름의 단계가 코드로 드러나게 한다.

### 6e8df39 Archunit 중복제거 리팩토링
- 주요 변경: Archunit 중복제거 리팩토링
- 핵심 코드: `common/src/testFixtures/java/com/example/common/archunit/HexagonalArchitectureTestTemplate.java`
```java
public abstract class HexagonalArchitectureTestTemplate {
//--- 생략 ...
    private HexagonalArchitectureRules rules() {
        return HexagonalArchitectureRules.getInstance(basePackage());
    }
//--- 생략 ...
}
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.

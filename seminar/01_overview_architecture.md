# 01. 전체 흐름과 초기 MSA 아키텍처

## 목표
- 프로젝트의 전체 비즈니스 흐름과 MSA 구성 요소를 이해한다.
- 주문 처리의 비동기/보상 흐름을 한 번에 조망한다.

## 스토리라인
- 단일 주문 프로세스를 쪼개면서 실패/보상/중복 문제가 등장한다.
- 이를 해결하기 위해 주문 오케스트레이터 + 쿠폰/포인트 MSA + 이벤트 기반 소비자를 구성한다.

## 관련 커밋(초기 아키텍처 골격)
- `a080f1d`, `82e897a`, `e37883c`, `79dec4c`, `193e5e2`

## 커밋 변경 요약
| 커밋 | 주요 변경 요약 | 체크아웃 |
| --- | --- | --- |
| `a080f1d` | order start | `git checkout a080f1d` |
| `82e897a` | 주문 오케스트레이터의 기본 골격만 완성. Persistent 개발 안됨 | `git checkout 82e897a` |
| `e37883c` | ### Common 모듈 추가 ###################### | `git checkout e37883c` |
| `79dec4c` | [Coupon-service]First commit | `git checkout 79dec4c` |
| `193e5e2` | First commit for Point MSA | `git checkout 193e5e2` |

## 핵심 개념
- MSA 분리 이유: 책임 분리, 장애 격리, 확장성
- EDA 도입 이유: 비동기 처리, 재시도, 사가 보상 가능
- 주요 컴포넌트: order-orchestrator, coupon-service, point-service, order-saga-consumer, common

## 기술/기능/프로세스
- 기술: Spring Boot 멀티 모듈, JPA, MySQL, Kafka
- 기능: 주문 생성, 예약/확정/보상 개념 정립
- MSA: order-orchestrator, coupon-service, point-service, order-saga-consumer, common
- EDA: order-saga-events 토픽 기반 이벤트 발행/소비
## 데모/실습
- 구조 확인: `readme.md`, `project_desc.md`
- 모듈 훑기: `settings.gradle`

## 데이터셋
- `seminar/support/datasets.md` 참고

## 커밋 상세
### a080f1d order start
- 주요 변경: order start
- 핵심 코드: `order-orchestrator/src/test/java/temptest/UUIDTest.java`
```java
public class UUIDTest {
//--- 생략 ...
    void generateUUIDv7_and_compareSortOrder() {
        compareID(() -> Generators.timeBasedGenerator().generate().toString());
    }
//--- 생략 ...
}
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.

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

### 79dec4c [Coupon-service]First commit
- 주요 변경: [Coupon-service]First commit
- 핵심 코드: `coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponService.java`
```java
public class ReserveCouponService implements ReserveCouponUseCase {
//--- 생략 ...
    public void reserve(String couponNumber, String orderId) {
        Coupon coupon = loadCouponPort.loadCoupon(couponNumber)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다: " + couponNumber));

        if (!coupon.isAvailable()) {
            throw new IllegalStateException("예약 불가능한 쿠폰입니다: " + couponNumber);
        }

        // 지금은 간단히 status만 RESERVED로 변경한 새 인스턴스를 만든다고 가정
        Coupon reserved = new Coupon(
                coupon.couponNumber(),
                CouponStatus.RESERVED,
                coupon.issuedAt(),
                coupon.expiredAt()
        );

        saveCouponPort.save(reserved);
    }
//--- 생략 ...
}
```
- 설명: 오케스트레이터가 쿠폰/포인트 예약을 병렬 호출해 분산 트랜잭션의 시작점을 만든다.

### 193e5e2 First commit for Point MSA
- 주요 변경: First commit for Point MSA
- 핵심 코드: `point-service/src/test/java/com/example/pointservice/application/service/ReservePointServiceTest.java`
```java
class ReservePointServiceTest {
//--- 생략 ...
    void reserve_shouldChangeStatusToReserved_andSave() {
        // given
        String pointNumber = "PNT-001";
        LocalDateTime now = LocalDateTime.now();
        Point availablePoint = new Point(pointNumber, PointStatus.AVAILABLE, now.minusDays(1), now.plusDays(1));

        when(loadPointPort.loadPoint(pointNumber)).thenReturn(Optional.of(availablePoint));

        // when
        reservePointService.reserve(pointNumber, "ORD-001");

        // then
        verify(loadPointPort, times(1)).loadPoint(pointNumber);
        verify(savePointPort, times(1)).save(argThat(saved ->
                saved.pointNumber().equals(pointNumber)
                        && saved.status() == PointStatus.RESERVED
        ));
    }
//--- 생략 ...
}
```
- 설명: 오케스트레이터가 쿠폰/포인트 예약을 병렬 호출해 분산 트랜잭션의 시작점을 만든다.

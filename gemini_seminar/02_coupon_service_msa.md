# Chapter 2: MSA 확장을 위한 Coupon Service 추가

## 1. 개요: 단일 서비스에서 MSA로의 전환

`order-orchestrator`라는 단일 서비스에서 시작한 우리 시스템은 이제 분산 환경으로 첫 발을 내딛습니다. 본 챕터에서는 새로운 마이크로서비스(MSA)인 `coupon-service`를 추가하고, 기존 `order-orchestrator`와 어떻게 상호작용하는지 살펴봅니다. 이 과정을 통해 MSA로의 전환이 가져다주는 이점과 함께, 새롭게 발생하는 과제들을 이해합니다.

### 핵심 학습 목표
*   마이크로서비스 아키텍처 도입의 필요성과 이점을 이해합니다.
*   `coupon-service`를 별도의 마이크로서비스로 분리하는 과정을 학습합니다.
*   MSA 간 동기(HTTP) 통신의 기본적인 구현 방식과 고려사항을 파악합니다.

## 2. 왜 마이크로서비스인가? MSA 도입의 필요성

주문 시스템이 복잡해지고 기능이 확장됨에 따라, 쿠폰, 포인트, 재고 등 다양한 하위 도메인이 등장합니다. 이 모든 비즈니스 로직을 하나의 서비스(Monolith)에서 관리할 경우 다음과 같은 문제에 직면하게 됩니다.

*   **높은 복잡도:** 코드베이스가 거대해져 이해하고 수정하기 어려워지고, 개발 생산성이 저하됩니다.
*   **낮은 배포 유연성:** 작은 기능 변경 하나를 배포하기 위해 전체 시스템을 다시 빌드하고 배포해야 하므로, 배포 주기가 길어지고 롤백의 위험이 커집니다.
*   **기술 스택 종속성:** 전체 시스템이 하나의 기술 스택에 얽매이게 되어, 새로운 기술 도입이 어렵고 특정 기술에 대한 의존도가 높아집니다.
*   **장애 전파 및 낮은 확장성:** 특정 기능에 장애가 발생하면 전체 서비스의 장애로 이어질 수 있으며, 특정 기능의 부하가 증가해도 전체 서비스를 확장해야 하므로 자원 효율성이 떨어집니다.

이러한 문제들을 해결하기 위해, 우리는 '쿠폰' 도메인을 별도의 `coupon-service`로 분리하기로 결정했습니다. `coupon-service`는 쿠폰과 관련된 모든 책임(생성, 조회, 예약, 확정, 보상 등)을 가집니다.

## 3. `coupon-service`의 탄생과 초기 Git 이력

`coupon-service`는 `order-orchestrator`와 마찬가지로 Hexagonal Architecture 원칙을 따르며 독립적으로 구축됩니다. 다음은 `coupon-service`의 초기 생성 및 `order-orchestrator`와의 연동 관련 주요 Git 커밋입니다.

| 커밋 ID | 날짜 | 주요 변경 요약 |
|---|---|---|
| `79dec4c` | 2025-12-16 | `coupon-service` 첫 커밋 (기본 골격) |
| `3103fe4` | 2025-12-17 | `ReserveCouponService` 단위 테스트 Mocking 추가 |
| `db4881a` | 2025-12-18 | `Coupon-service` 연계 통합 테스트 추가 |
| `58d7578` | 2025-12-29 | `schema.sql` 실행 이슈 관련 테스트 오류 수정 |

**(실습 가이드: Git 커밋 확인)**
1.  프로젝트 루트에서 `git log --oneline --grep="coupon-service" --grep="db4881a"` 명령어를 실행하여 `coupon-service` 관련 커밋들을 직접 확인해 보세요.
2.  `git checkout db4881a` 명령어로 해당 커밋 시점으로 이동하여 `order-orchestrator`와 `coupon-service`가 연동된 통합 테스트 코드를 확인해 볼 수 있습니다. (확인 후 `git checkout main` 등으로 돌아오세요.)

## 4. 핵심 코드 스니펫: MSA 간 동기(HTTP) 통신

### 4.1. `order-orchestrator`에서 `coupon-service` 호출 (Driving Adapter → Driven Adapter)

`order-orchestrator`는 `coupon-service`를 호출하기 위한 Output Port (`ReserveCouponPort`)와 이를 구현한 `WebClient` 기반의 `CouponServiceClient` (Driven Adapter)를 사용합니다.

**`order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/webclient/CouponServiceClient.java`**
```java
// ... imports ...
@Component
public class CouponServiceClient implements ReserveCouponPort { // Output Port 구현체

    private final WebClient webClient;

    public CouponServiceClient(
            WebClient.Builder builder,
            @Value("${external.coupon.base-url}") String baseUrl // application.yml 에서 설정
    ) {
        // application.yml 에 정의된 coupon-service 의 주소(ex: http://localhost:8081)로 WebClient 를 생성
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public Mono<Void> reserveCoupon(String couponNumber, String orderId) {
        ReserveCouponRequest request = new ReserveCouponRequest(couponNumber, orderId);

        // HTTP POST 요청으로 coupon-service 의 /api/v1/coupons/reserve 엔드포인트 호출
        return webClient.post()
                .uri("/api/v1/coupons/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<WebApiResponse<ReserveCouponResponse>>() {}) // 응답 DTO 매핑
                .flatMap(response -> {
                    if (response.isSuccess()) {
                        return Mono.empty(); // 성공 시 Mono<Void> 반환
                    }
                    // 실패 응답 시 예외 발생
                    return Mono.error(new IllegalStateException("Coupon reservation failed: " + response.getError().getMessage()));
                })
                .then(); // Mono<Void> 로 최종 변환
    }
}
```
**설명:** `order-orchestrator`는 `application.yml`에 설정된 `external.coupon.base-url`을 통해 `coupon-service`의 주소를 가져와 `WebClient`를 초기화합니다. `reserveCoupon` 메서드는 비동기적으로 HTTP POST 요청을 보내 쿠폰 예약을 시도하고, 응답 결과에 따라 성공 또는 실패를 처리합니다.

### 4.2. `coupon-service`에서 쿠폰 예약 처리 (Driving Adapter → Application Layer)

`coupon-service`는 `order-orchestrator`로부터의 HTTP 요청을 `CouponController` (Driving Adapter)가 받아 `ReserveCouponUseCase` (Input Port)를 호출하고, `ReserveCouponService` (Application Layer)가 실제 비즈니스 로직을 처리합니다.

**`coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`**
```java
// ... imports ...
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController { // Driving Adapter

    private final ReserveCouponUseCase reserveCouponUseCase; // Input Port

    @PostMapping("/reserve")
    public ApiResponse<ReserveCouponResponse> reserveCoupon(@RequestBody ReserveCouponRequest request) {
        reserveCouponUseCase.reserve(request.couponNumber(), request.orderId()); // UseCase 호출

        // 성공 응답 반환
        return ApiResponse.success(buildReserveResponse(request.couponNumber(), CouponStatus.RESERVED));
    }
    // ... (buildReserveResponse 등 헬퍼 메서드 생략)
}
```

**`coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponService.java`**
```java
// ... imports ...
@Service
@RequiredArgsConstructor
@Transactional
public class ReserveCouponService implements ReserveCouponUseCase { // Input Port 구현체

    private final LoadCouponPort loadCouponPort; // Output Port
    private final SaveCouponPort saveCouponPort; // Output Port
    private final LoadCouponReservationPort loadCouponReservationPort; // Output Port (타이밍 이슈 해결용)
    private final SaveCouponReservationPort saveCouponReservationPort; // Output Port (타이밍 이슈 해결용)

    @Override
    public void reserve(String couponNumber, String orderId) {
        // [타이밍 이슈 해결 로직] 이미 보상 처리된 주문이거나 이미 예약된 주문인지 확인
        // 이 부분은 Chapter 7에서 자세히 다룹니다.
        if (isReservationCancelled(orderId)) { // 예약이 이미 취소되었다면 (보상 먼저 도착)
            return; // 더 이상 진행하지 않고 바로 리턴 (멱등성 확보)
        }
        verifyReservationNotAlreadyReserved(orderId); // 이미 예약되었다면 예외 발생 (멱등성 확보)

        Coupon coupon = loadCouponPort.loadCoupon(couponNumber) // Output Port를 통해 쿠폰 조회
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다: " + couponNumber));

        if (!coupon.isAvailable()) { // 쿠폰 사용 가능 여부 확인 (도메인 로직)
            throw new IllegalStateException("예약 불가능한 쿠폰입니다: " + couponNumber);
        }

        // 쿠폰 상태를 'RESERVED'로 변경 (도메인 로직)
        Coupon reservedCoupon = new Coupon(
                coupon.couponNumber(),
                CouponStatus.RESERVED,
                coupon.issuedAt(),
                coupon.expiredAt()
        );
        saveCouponPort.save(reservedCoupon); // Output Port를 통해 쿠폰 상태 저장

        // [타이밍 이슈 해결 로직] 쿠폰 예약 정보 저장
        saveCouponReservationPort.saveReservation(new CouponReservation(
                orderId,
                couponNumber,
                ReservationStatus.RESERVED
        ));
    }
    // ... (confirm, compensate 및 헬퍼 메서드 생략)
}
```
**설명:** `ReserveCouponService`는 쿠폰 조회, 유효성 검사, 상태 변경, 예약 정보 저장 등 쿠폰 예약과 관련된 핵심 비즈니스 로직을 수행합니다. 여기서 `isReservationCancelled`나 `verifyReservationNotAlreadyReserved` 같은 메서드는 분산 시스템에서 발생할 수 있는 타이밍 이슈(보상 요청이 예약 요청보다 먼저 도착하는 등)를 해결하기 위한 로직이며, 이는 Chapter 7에서 더 깊이 다룰 예정입니다.

## 5. 실습 체크포인트

### 5.1. `coupon-service` 실행 및 API 호출
1.  **`coupon-service` 빌드 및 실행:**
    *   프로젝트 루트에서 `bin_common/00_prepare_mysql_kafka.sh`를 실행하여 MySQL과 Kafka를 준비합니다. (처음 한 번만 실행)
    *   새로운 터미널을 열고 `coupon-service` 프로젝트 폴더(`coupon-service/`)로 이동한 후 `./gradlew bootRun` 명령어로 `coupon-service`를 실행합니다. (또는 IDE에서 `CouponServiceApplication.java`를 실행)
    *   `coupon-service`는 기본적으로 `8081` 포트로 실행됩니다.
2.  **`order-orchestrator` 실행:**
    *   새로운 터미널을 열고 `order-orchestrator` 프로젝트 폴더(`order-orchestrator/`)로 이동한 후 `./gradlew bootRun` 명령어로 `order-orchestrator`를 실행합니다. (또는 IDE에서 `OrderOrchestratorApplication.java`를 실행)
    *   `order-orchestrator`는 기본적으로 `8080` 포트로 실행됩니다. `application.yml`의 `external.coupon.base-url`이 `http://localhost:8081`로 설정되어 있는지 확인하세요.
3.  **주문 생성 API 호출:**
    *   `order-orchestrator/src/test/httprequest/01_orderOrchestratorTest.http` 파일을 엽니다.
    *   "주문 생성 요청 (Happy Path 예시)" 부분을 찾아 HTTP 클라이언트로 요청을 보냅니다 (예: VS Code의 REST Client 확장).
    *   **요청 바디 예시:**
        ```json
        {
          "couponNumber": "CPN-INT-AVAILABLE-001",
          "pointNumber": "PNT-INT-AVAILABLE-001",
          "paymentNumber": "PAY-001",
          "paymentAmount": 35000,
          "orderItems": [
            {
              "itemNumber": "ITEM-001",
              "quantity": 2
            }
          ]
        }
        ```
    *   **예상 결과:** `HTTP 200 OK` 응답을 받으며, `orderId`와 `sagaId`가 반환됩니다. `coupon-service`의 로그에서 쿠폰 예약 처리 관련 메시지를 확인할 수 있습니다.
4.  **H2 Console을 통해 DB 확인 (선택 사항):**
    *   브라우저에서 `http://localhost:8080/h2-console`에 접속하여 `order-orchestrator`의 H2 DB에 접근합니다.
    *   `SELECT * FROM ORDER_SAGA;`와 `SELECT * FROM OUTBOX_MESSAGE;`를 실행하여 새로운 주문과 아웃박스 메시지가 생성되었는지 확인합니다.
    *   `coupon-service`의 H2 DB (만약 설정되어 있다면)에 접속하여 `SELECT * FROM COUPON;`을 실행하여 쿠폰 상태가 `RESERVED`로 변경되었는지 확인합니다.

---
이 단계를 통해 우리는 첫 번째 마이크로서비스를 성공적으로 분리하고 연동했습니다. 하지만 서비스 간 동기 통신은 여전히 장애 전파의 위험을 안고 있으며, 분산 트랜잭션의 데이터 정합성 문제는 해결되지 않았습니다. 다음 챕터에서는 이 문제를 해결하기 위한 핵심 패턴인 `Outbox Pattern`에 대해 깊이 있게 다룹니다.
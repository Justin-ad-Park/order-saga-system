# Chapter 2: MSA 확장을 위한 Coupon Service 추가

`order-orchestrator`라는 단일 서비스에서 시작한 우리 시스템은 이제 분산 환경으로 첫 발을 내딛습니다. 본 챕터에서는 새로운 마이크로서비스(MSA)인 `coupon-service`를 추가하고, 기존 `order-orchestrator`와 어떻게 상호작용하는지 살펴봅니다.

## 1. 왜 마이크로서비스인가?

주문 시스템이 복잡해지면서 쿠폰, 포인트, 재고 등 다양한 하위 도메인이 생겨납니다. 이 모든 것을 하나의 거대한 서비스(Monolith)에서 관리하면 다음과 같은 문제가 발생할 수 있습니다.

*   **높은 복잡도:** 코드베이스가 비대해져 이해하고 수정하기 어렵습니다.
*   **낮은 배포 유연성:** 작은 수정사항 하나를 배포하기 위해 전체 시스템을 다시 빌드하고 배포해야 합니다.
*   **기술 종속성:** 전체 시스템이 하나의 기술 스택에 얽매이게 됩니다.
*   **장애 전파:** 특정 기능의 장애가 전체 서비스의 장애로 이어질 수 있습니다.

이러한 문제를 해결하기 위해, 우리는 '쿠폰' 도메인을 별도의 `coupon-service`로 분리하기로 결정했습니다.

## 2. Coupon Service의 탄생

`coupon-service`는 쿠폰과 관련된 모든 책임(생성, 조회, 사용, 차감)을 가집니다. 이 서비스 역시 `order-orchestrator`와 마찬가지로 Hexagonal Architecture를 따릅니다.

이제 주문 시 쿠폰을 사용하는 흐름은 다음과 같이 변경됩니다.
1.  클라이언트가 `order-orchestrator`에 주문을 요청합니다.
2.  `order-orchestrator`는 주문을 생성한 후, HTTP 클라이언트를 통해 `coupon-service`에 쿠폰 사용을 요청(동기 호출)합니다.
3.  `coupon-service`는 쿠폰의 유효성을 검사하고, 사용 처리 후 결과를 `order-orchestrator`에 응답합니다.

## 3. 주요 Git 이력

아래 커밋들은 `coupon-service`를 추가하고 `order-orchestrator`와 연동하는 과정을 보여줍니다.
```
* db4881a | 2025-12-18 | Coupon-service 연계 통합 테스트
* 79dec4c | 2025-12-16 | [Coupon-service]First commit
* e37883c | 2025-12-15 | ### Common 모듈 추가 ######################
```

## 4. 핵심 코드 스니펫

### 서비스 간 동기 호출 (HTTP Request/Response)

`order-orchestrator`는 `coupon-service`를 호출하기 위한 Output Adapter로 `WebClient`를 사용하는 `CouponServiceClient`를 구현했습니다.

**`order-orchestrator/.../out/webclient/CouponServiceClient.java` (호출 측)**
```java
@Component
public class CouponServiceClient implements ReserveCouponPort {

    private final WebClient webClient;

    public CouponServiceClient(
            WebClient.Builder builder,
            @Value("${external.coupon.base-url}") String baseUrl
    ) {
        // application.yml 에 정의된 coupon-service 의 주소로 WebClient 를 생성
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public Mono<Void> reserveCoupon(String couponNumber, String orderId) {
        ReserveCouponRequest request = new ReserveCouponRequest(couponNumber, orderId);

        // HTTP POST 요청으로 coupon-service 에 쿠폰 예약을 요청
        return webClient.post()
                .uri("/api/v1/coupons/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<WebApiResponse<ReserveCouponResponse>>() {})
                .then();
    }
}
```

`coupon-service`는 이 요청을 받아 처리하는 Input Adapter로 `CouponController`를 가지고 있습니다.

**`coupon-service/.../in/web/CouponController.java` (응답 측)**
```java
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final ReserveCouponUseCase reserveCouponUseCase;

    @PostMapping("/reserve")
    public ApiResponse<ReserveCouponResponse> reserveCoupon(@RequestBody ReserveCouponRequest request) {
        // 실제 비즈니스 로직은 UseCase 에 위임
        reserveCouponUseCase.reserve(request.couponNumber(), request.orderId());

        return ApiResponse.success(buildReserveResponse(request.couponNumber(), CouponStatus.RESERVED));
    }
    // ...
}
```

### 쿠폰 예약 비즈니스 로직

`coupon-service`의 `ReserveCouponService`는 `ReserveCouponUseCase` 인터페이스를 구현하며, 실제 쿠폰을 예약 처리하는 핵심 비즈니스 로직을 담고 있습니다.

**`coupon-service/.../application/service/ReserveCouponService.java`**
```java
@Service
@RequiredArgsConstructor
@Transactional
public class ReserveCouponService implements ReserveCouponUseCase, ConfirmCouponUseCase, CompensateCouponUseCase {

    private final LoadCouponPort loadCouponPort;
    private final SaveCouponPort saveCouponPort;
    private final LoadCouponReservationPort loadCouponReservationPort;
    private final SaveCouponReservationPort saveCouponReservationPort;

    @Override
    public void reserve(String couponNumber, String orderId) {
        // 이미 보상 처리된 주문이거나 이미 예약된 주문인지 확인
        if (isReservationCancelled(orderId)) {
            return;
        }
        verifyReservationNotAlreadyReserved(orderId);

        // 쿠폰 상태를 RESERVED로 업데이트하고 유효성 검사
        updateStatus(couponNumber, CouponStatus.RESERVED, this::validateReservable);
        
        // 쿠폰 예약 정보 저장
        saveCouponReservationPort.saveReservation(new CouponReservation(
                orderId,
                couponNumber,
                ReservationStatus.RESERVED
        ));
    }
    // ... (confirm, compensate 및 헬퍼 메서드 생략)
}
```
이처럼 두 서비스는 표준 HTTP 프로토콜을 통해 통신하며, 각자의 내부 구현(Hexagonal Architecture)은 숨긴 채 정해진 API 명세로만 상호작용합니다.

---
이 단계를 통해 우리는 처음으로 두 개의 서비스가 상호작용하는 MSA 구조를 구축했습니다. 하지만 여기서 새로운 문제가 발생합니다. 만약 주문은 성공했는데 쿠폰 사용 처리는 실패하면 어떻게 될까요? 데이터 정합성 문제가 발생하게 됩니다. 다음 챕터에서는 이 문제를 해결하기 위한 `Outbox Pattern`에 대해 알아봅니다.

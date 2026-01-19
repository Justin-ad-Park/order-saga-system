# 04. 쿠폰 서비스 구축과 예약 흐름

## 목표
- 쿠폰 서비스의 기본 예약 흐름을 이해한다.

## 스토리라인
- 주문을 분해하면서 쿠폰 서비스부터 독립적으로 구축.

## 관련 커밋
- `79dec4c`, `3103fe4`, `db4881a`, `95df8c2`, `58d7578`

## 커밋 변경 요약
| 커밋 | 주요 변경 요약 | 체크아웃 |
| --- | --- | --- |
| `79dec4c` | [Coupon-service]First commit | `git checkout 79dec4c` |
| `3103fe4` | ReserveCouponServiceTest Mock | `git checkout 3103fe4` |
| `db4881a` | Coupon-service 연계 통합 테스트 | `git checkout db4881a` |
| `95df8c2` | 통합 테스트 개선 | `git checkout 95df8c2` |
| `58d7578` | schema.sql 실행 이슈 관련 테스트 오류 수정 | `git checkout 58d7578` |

## 핵심 개념
- 예약/확정/보상의 상태 전이
- 테스트 데이터 초기화 전략

## 기술/기능/프로세스
- 기술: Spring Boot, JPA, MySQL, REST
- 기능: reserve/confirm/compensate, reservation 상태
- MSA: 쿠폰 서비스 독립 배포
- EDA: 오케스트레이터 호출 결과를 이벤트로 확장 가능
## 데모/실습
- 테스트 데이터 확인: `coupon-service/src/main/resources/coupon_schema.sql`
- 통합 테스트: `coupon-service/src/test/java/.../CouponControllerIntegrationTest.java`

## 커밋 상세
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

### 3103fe4 ReserveCouponServiceTest Mock
- 주요 변경: ReserveCouponServiceTest Mock
- 핵심 코드: `coupon-service/src/test/java/com/example/couponservice/application/service/ReserveCouponServiceTest.java`
```java
class ReserveCouponServiceTest {
//--- 생략 ...
    void reserve_shouldChangeStatusToReserved_andSave() {
        // given
        String couponNumber = "CPN-001";
        LocalDateTime now = LocalDateTime.now();
        Coupon availableCoupon = new Coupon(couponNumber, CouponStatus.AVAILABLE, now.minusDays(1), now.plusDays(1));

        when(loadCouponPort.loadCoupon(couponNumber)).thenReturn(Optional.of(availableCoupon));

        // when
        reserveCouponService.reserve(couponNumber, "ORD-001");

        // then
        verify(loadCouponPort, times(1)).loadCoupon(couponNumber);
        verify(saveCouponPort, times(1)).save(argThat(saved ->
                saved.couponNumber().equals(couponNumber)
                        && saved.status() == CouponStatus.RESERVED
        ));
    }
//--- 생략 ...
}
```
- 설명: 오케스트레이터가 쿠폰/포인트 예약을 병렬 호출해 분산 트랜잭션의 시작점을 만든다.

### db4881a Coupon-service 연계 통합 테스트
- 주요 변경: Coupon-service 연계 통합 테스트
- 핵심 코드: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/webclient/dto/ReserveCouponResponse.java`
```java
public record ReserveCouponResponse(
        String couponNumber,
        String status
) {
}
//--- 생략 ...
```
- 설명: 오케스트레이터가 쿠폰/포인트 예약을 병렬 호출해 분산 트랜잭션의 시작점을 만든다.

### 95df8c2 통합 테스트 개선
- 주요 변경: 통합 테스트 개선
- 핵심 코드: `coupon-service/src/main/java/com/example/couponservice/config/TestDataCleaner.java`
```java
//public class TestDataCleaner implements ApplicationRunner {
//--- 생략 ...
//}
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.

### 58d7578 schema.sql 실행 이슈 관련 테스트 오류 수정
- 주요 변경: schema.sql 실행 이슈 관련 테스트 오류 수정
- 핵심 코드: `coupon-service/src/test/java/com/example/couponservice/adapter/in/web/CouponControllerIntegrationTest.java`
```java
class CouponControllerIntegrationTest {
//--- 생략 ...
    void reserveCoupon_shouldChangeStatusToReserved_andReturn200() {
        // given
        String couponNumber = "C-001";

        //makeTestCoupon(couponNumber);

        String url = "http://localhost:" + port + "/api/v1/coupons/reserve";

        ReserveCouponRequest requestBody =
                new ReserveCouponRequest(couponNumber, "ORD-12345");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ReserveCouponRequest> httpEntity =
                new HttpEntity<>(requestBody, headers);

        // when
        ResponseEntity<String> response =
                restTemplate.postForEntity(url, httpEntity, String.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        CouponJpaEntity updated =
                couponJpaRepository.findById(couponNumber).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(CouponStatus.RESERVED);
    }
//--- 생략 ...
}
```
- 설명: 오케스트레이터가 쿠폰/포인트 예약을 병렬 호출해 분산 트랜잭션의 시작점을 만든다.

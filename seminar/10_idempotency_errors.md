# 10. 멱등성, 오류 메시지, API 확장

## 목표
- API 멱등성과 오류 처리 정책을 이해한다.

## 스토리라인
- 중복 요청과 보상 요청이 반복되면서, 오류 메시지와 멱등 처리가 중요해짐.

## 관련 커밋
- `542ed97`, `091c2a7`, `66c93ca`, `35b85e3`, `605354d`, `7d9e662`

## 커밋 변경 요약
| 커밋 | 주요 변경 요약 | 체크아웃 |
| --- | --- | --- |
| `542ed97` | ### Coupon-service confirm API 추가 ### | `git checkout 542ed97` |
| `091c2a7` | coupon-service에 보상(compensateCoupon) API 추가 | `git checkout 091c2a7` |
| `66c93ca` | confirm, compansate API를 point-service에도 동일한 방식으로 추가 | `git checkout 66c93ca` |
| `35b85e3` | API 응답 에러 명시적으로 변경 중 | `git checkout 35b85e3` |
| `605354d` | coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장 | `git checkout 605354d` |
| `7d9e662` | 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성) | `git checkout 7d9e662` |

## 핵심 개념
- confirm/compensate API 설계
- 멱등성 정책(이미 처리된 요청)

## 기술/기능/프로세스
- 기술: 예외 처리, HTTP 상태 코드 설계
- 기능: 멱등 confirm/compensate, 명확한 오류 메시지
- MSA: 쿠폰/포인트 공통 정책 정립
- EDA: 재시도/중복 이벤트 대비
## 데모/실습
- HTTP 테스트 파일 확인: `coupon-service/src/test/resources/01_couponServiceTest.http`, `point-service/src/test/resources/01_pointServiceTest.http`

## 커밋 상세
### 542ed97 ### Coupon-service confirm API 추가 ###
- 주요 변경: ### Coupon-service confirm API 추가 ###
- 핵심 코드: `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/config/KafkaConsumerConfig.java`
```java
//--- 생략 ...
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.

### 091c2a7 coupon-service에 보상(compensateCoupon) API 추가
- 주요 변경: coupon-service에 보상(compensateCoupon) API 추가
- 핵심 코드: `coupon-service/src/test/java/com/example/couponservice/application/service/ReserveCouponServiceTest.java`
```java
class ReserveCouponServiceTest {
//--- 생략 ...
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("확정 불가능한 쿠폰");

        verify(saveCouponPort, never()).save(any());
    }

    @Test
    void compensate_shouldChangeStatusToCompensated_andSave() {
//--- 생략 ...
}
```
- 설명: 오케스트레이터가 쿠폰/포인트 예약을 병렬 호출해 분산 트랜잭션의 시작점을 만든다.

### 66c93ca confirm, compansate API를 point-service에도 동일한 방식으로 추가
- 주요 변경: confirm, compansate API를 point-service에도 동일한 방식으로 추가
- 핵심 코드: `point-service/src/test/java/com/example/pointservice/application/service/ReservePointServiceTest.java`
```java
class ReservePointServiceTest {
//--- 생략 ...
        verify(savePointPort, times(1)).save(argThat(saved ->
                saved.pointNumber().equals(pointNumber)
                        && saved.status() == PointStatus.RESERVED
        ));
    }

    @Test
    void confirm_shouldChangeStatusToUsed_andSave() {
//--- 생략 ...
}
```
- 설명: 오케스트레이터가 쿠폰/포인트 예약을 병렬 호출해 분산 트랜잭션의 시작점을 만든다.

### 35b85e3 API 응답 에러 명시적으로 변경 중
- 주요 변경: API 응답 에러 명시적으로 변경 중
- 핵심 코드: `point-service/src/test/java/com/example/pointservice/application/service/ReservePointServiceTest.java`
```java
class ReservePointServiceTest {
//--- 생략 ...
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("확정 불가능한 포인트");

        verify(savePointPort, never()).save(any());
    }

    @Test
    void compensate_shouldChangeStatusToAvailable_andSave() {
//--- 생략 ...
}
```
- 설명: 오케스트레이터가 쿠폰/포인트 예약을 병렬 호출해 분산 트랜잭션의 시작점을 만든다.

### 605354d coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 주요 변경: coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 핵심 코드: `coupon-service/src/test/java/com/example/couponservice/adapter/in/web/CouponControllerIntegrationTest.java`
```java
class CouponControllerIntegrationTest {
//--- 생략 ...
    void confirmCoupon_shouldChangeStatusToUsed_whenReserved() {
        String couponNumber = "CPN-INT-CONFIRM-001";
        makeTestCoupon(couponNumber);

        String reserveUrl = "http://localhost:" + port + "/api/v1/coupons/reserve";
        String confirmUrl = "http://localhost:" + port + "/api/v1/coupons/confirm";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ReserveCouponRequest reserveRequest =
                new ReserveCouponRequest(couponNumber, "ORD-12345");
        ResponseEntity<String> reserveResponse =
                restTemplate.postForEntity(
                        reserveUrl,
                        new HttpEntity<>(reserveRequest, headers),
                        String.class
                );
        assertThat(reserveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ConfirmCouponRequest confirmRequest =
                new ConfirmCouponRequest(couponNumber, "ORD-12345");
        ResponseEntity<String> confirmResponse =
                restTemplate.postForEntity(
                        confirmUrl,
                        new HttpEntity<>(confirmRequest, headers),
                        String.class
                );

        assertThat(confirmResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        CouponJpaEntity updated =
                couponJpaRepository.findById(couponNumber).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(CouponStatus.USED);
    }
//--- 생략 ...
}
```
- 설명: 오케스트레이터가 쿠폰/포인트 예약을 병렬 호출해 분산 트랜잭션의 시작점을 만든다.

### 7d9e662 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 주요 변경: 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 핵심 코드: `order-saga-consumer/src/main/resources/OSC_application.yaml`
```yaml
//--- 생략 ...
  saga:
    events:
      topic: order-saga-events-test

---
spring:
  config:
    activate:
      on-profile: dev
//--- 생략 ...
```
- 설명: Kafka/Consumer 배포 설정을 추가해 실행 환경을 고정한다.
## 1) OrderSagaConsumer 모듈 추가 및 기본 컨슈머 구성
- 새 모듈 `order-saga-consumer` 추가(기존 `OrderSagaConsumer`에서 이름 변경).
//--- 생략 ...
```
- 설명: Kafka 컨슈머가 이벤트를 수신해 confirm/compensate 흐름을 이어간다.

### 35b85e3 API 응답 에러 명시적으로 변경 중
- 주요 변경: API 응답 에러 명시적으로 변경 중
- 핵심 코드: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`
```java
public class CouponController {
//--- 생략 ...
    public ApiResponse<ConfirmCouponResponse> confirmCoupon(@RequestBody ConfirmCouponRequest request) {
        confirmCouponUseCase.confirm(request.couponNumber(), request.orderId());
        return ApiResponse.success(buildConfirmResponse(request.couponNumber(), CouponStatus.USED));
    }
//--- 생략 ...
}
```
- 설명: 성공 시 confirm 트랜잭션으로 예약을 확정한다.

### 605354d coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 주요 변경: coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 핵심 코드: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/GlobalExceptionHandler.java`
```java
public class GlobalExceptionHandler {
//--- 생략 ...
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(IllegalArgumentException ex) {
        return responseEntityWithHttpStatus(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }
//--- 생략 ...
}
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.

### 7d9e662 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 주요 변경: 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 핵심 코드: `bin_k8s/07_run_local_consumer.sh`
```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

//--- 생략 ...
```
- 설명: 실행/테스트 스크립트를 통해 분산 시나리오를 재현한다.
## 1) OrderSagaConsumer 모듈 추가 및 기본 컨슈머 구성
- 새 모듈 `order-saga-consumer` 추가(기존 `OrderSagaConsumer`에서 이름 변경).
//--- 생략 ...
```
- 설명: Kafka 컨슈머가 이벤트를 수신해 confirm/compensate 흐름을 이어간다.

### 35b85e3 API 응답 에러 명시적으로 변경 중
- 주요 변경: API 응답 에러 명시적으로 변경 중
- 핵심 코드: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`
```java
public class CouponController {
//--- 생략 ...
    public ApiResponse<ConfirmCouponResponse> confirmCoupon(@RequestBody ConfirmCouponRequest request) {
        confirmCouponUseCase.confirm(request.couponNumber(), request.orderId());
        return ApiResponse.success(buildConfirmResponse(request.couponNumber(), CouponStatus.USED));
    }
//--- 생략 ...
}
```
- 설명: 성공 시 confirm 트랜잭션으로 예약을 확정한다.

### 605354d coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 주요 변경: coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 핵심 코드: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/GlobalExceptionHandler.java`
```java
public class GlobalExceptionHandler {
//--- 생략 ...
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(IllegalArgumentException ex) {
        return responseEntityWithHttpStatus(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }
//--- 생략 ...
}
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.

### 7d9e662 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 주요 변경: 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 핵심 코드: `bin_k8s/07_run_local_consumer.sh`
```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

//--- 생략 ...
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.
## 1) OrderSagaConsumer 모듈 추가 및 기본 컨슈머 구성
- 새 모듈 `order-saga-consumer` 추가(기존 `OrderSagaConsumer`에서 이름 변경).
//--- 생략 ...
```
- 설명: Kafka 컨슈머가 이벤트를 수신해 confirm/compensate 흐름을 이어간다.

### 35b85e3 API 응답 에러 명시적으로 변경 중
- 주요 변경: API 응답 에러 명시적으로 변경 중
- 핵심 코드: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`
```java
public class CouponController {
//--- 생략 ...
    public ApiResponse<CompensateCouponResponse> compensateCoupon(@RequestBody CompensateCouponRequest request) {
        compensateCouponUseCase.compensateCoupon(request.couponNumber(), request.orderId());
        return ApiResponse.success(buildCompensateResponse(request.couponNumber(), CouponStatus.AVAILABLE));
    }
//--- 생략 ...
}
```
- 설명: 실패 시 보상 트랜잭션을 수행해 상태를 원복한다.

### 605354d coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 주요 변경: coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 핵심 코드: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/GlobalExceptionHandler.java`
```java
public class GlobalExceptionHandler {
//--- 생략 ...
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(IllegalArgumentException ex) {
        return responseEntityWithHttpStatus(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }
//--- 생략 ...
}
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.

### 7d9e662 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 주요 변경: 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 핵심 코드: `bin_k8s/07_run_local_consumer.sh`
```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

//--- 생략 ...
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.
## 1) OrderSagaConsumer 모듈 추가 및 기본 컨슈머 구성
- 새 모듈 `order-saga-consumer` 추가(기존 `OrderSagaConsumer`에서 이름 변경).
//--- 생략 ...
```
- 설명: Kafka 컨슈머가 이벤트를 수신해 confirm/compensate 흐름을 이어간다.

### 35b85e3 API 응답 에러 명시적으로 변경 중
- 주요 변경: API 응답 에러 명시적으로 변경 중
- 핵심 코드: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`
```java
public class CouponController {
//--- 생략 ...
    public ApiResponse<CompensateCouponResponse> compensateCoupon(@RequestBody CompensateCouponRequest request) {
        compensateCouponUseCase.compensateCoupon(request.couponNumber(), request.orderId());
        return ApiResponse.success(buildCompensateResponse(request.couponNumber(), CouponStatus.AVAILABLE));
    }
//--- 생략 ...
}
```
- 설명: 실패 시 보상 트랜잭션을 수행해 상태를 원복한다.

### 605354d coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 주요 변경: coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 핵심 코드: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/GlobalExceptionHandler.java`
```java
public class GlobalExceptionHandler {
//--- 생략 ...
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(IllegalArgumentException ex) {
        return responseEntityWithHttpStatus(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }
//--- 생략 ...
}
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.

### 7d9e662 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 주요 변경: 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 핵심 코드: `bin_k8s/07_run_local_consumer.sh`
```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

//--- 생략 ...
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.
## 1) OrderSagaConsumer 모듈 추가 및 기본 컨슈머 구성
- 새 모듈 `order-saga-consumer` 추가(기존 `OrderSagaConsumer`에서 이름 변경).
//--- 생략 ...
```
- 설명: Kafka 컨슈머가 이벤트를 수신해 confirm/compensate 흐름을 이어간다.

### 35b85e3 API 응답 에러 명시적으로 변경 중
- 주요 변경: API 응답 에러 명시적으로 변경 중
- 핵심 코드: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`
```java
public class CouponController {
//--- 생략 ...
    public ApiResponse<CompensateCouponResponse> compensateCoupon(@RequestBody CompensateCouponRequest request) {
        compensateCouponUseCase.compensateCoupon(request.couponNumber(), request.orderId());
        return ApiResponse.success(buildCompensateResponse(request.couponNumber(), CouponStatus.AVAILABLE));
    }
//--- 생략 ...
}
```
- 설명: 실패 시 보상 트랜잭션을 수행해 상태를 원복한다.

### 605354d coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 주요 변경: coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 핵심 코드: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/GlobalExceptionHandler.java`
```java
public class GlobalExceptionHandler {
//--- 생략 ...
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(IllegalArgumentException ex) {
        return responseEntityWithHttpStatus(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }
//--- 생략 ...
}
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.

### 7d9e662 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 주요 변경: 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 핵심 코드: `bin_k8s/07_run_local_consumer.sh`
```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

//--- 생략 ...
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.
## 1) OrderSagaConsumer 모듈 추가 및 기본 컨슈머 구성
- 새 모듈 `order-saga-consumer` 추가(기존 `OrderSagaConsumer`에서 이름 변경).
//--- 생략 ...
```
- 설명: Kafka 컨슈머가 이벤트를 수신해 confirm/compensate 흐름을 이어간다.

### 35b85e3 API 응답 에러 명시적으로 변경 중
- 주요 변경: API 응답 에러 명시적으로 변경 중
- 핵심 코드: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`
```java
public class CouponController {
//--- 생략 ...
    public ApiResponse<CompensateCouponResponse> compensateCoupon(@RequestBody CompensateCouponRequest request) {
        compensateCouponUseCase.compensateCoupon(request.couponNumber(), request.orderId());
        return ApiResponse.success(buildCompensateResponse(request.couponNumber(), CouponStatus.AVAILABLE));
    }
//--- 생략 ...
}
```
- 설명: 실패 시 보상 트랜잭션을 수행해 상태를 원복한다.

### 605354d coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 주요 변경: coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 핵심 코드: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/GlobalExceptionHandler.java`
```java
public class GlobalExceptionHandler {
//--- 생략 ...
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(IllegalArgumentException ex) {
        return responseEntityWithHttpStatus(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }
//--- 생략 ...
}
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.

### 7d9e662 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 주요 변경: 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 핵심 코드: `bin_k8s/07_run_local_consumer.sh`
```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

//--- 생략 ...
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.
## 1) OrderSagaConsumer 모듈 추가 및 기본 컨슈머 구성
- 새 모듈 `order-saga-consumer` 추가(기존 `OrderSagaConsumer`에서 이름 변경).
//--- 생략 ...
```
- 설명: Kafka 컨슈머가 이벤트를 수신해 confirm/compensate 흐름을 이어간다.

### 35b85e3 API 응답 에러 명시적으로 변경 중
- 주요 변경: API 응답 에러 명시적으로 변경 중
- 핵심 코드: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`
```java
public class CouponController {
//--- 생략 ...
    public ApiResponse<CompensateCouponResponse> compensateCoupon(@RequestBody CompensateCouponRequest request) {
        compensateCouponUseCase.compensateCoupon(request.couponNumber(), request.orderId());
        return ApiResponse.success(buildCompensateResponse(request.couponNumber(), CouponStatus.AVAILABLE));
    }
//--- 생략 ...
}
```
- 설명: 실패 시 보상 트랜잭션을 수행해 상태를 원복한다.

### 605354d coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 주요 변경: coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 핵심 코드: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/GlobalExceptionHandler.java`
```java
public class GlobalExceptionHandler {
//--- 생략 ...
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(IllegalArgumentException ex) {
        return responseEntityWithHttpStatus(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }
//--- 생략 ...
}
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.

### 7d9e662 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 주요 변경: 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 핵심 코드: `bin_k8s/07_run_local_consumer.sh`
```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

//--- 생략 ...
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.
## 1) OrderSagaConsumer 모듈 추가 및 기본 컨슈머 구성
- 새 모듈 `order-saga-consumer` 추가(기존 `OrderSagaConsumer`에서 이름 변경).
//--- 생략 ...
```
- 설명: Kafka 컨슈머가 이벤트를 수신해 confirm/compensate 흐름을 이어간다.

### 35b85e3 API 응답 에러 명시적으로 변경 중
- 주요 변경: API 응답 에러 명시적으로 변경 중
- 핵심 코드: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`
```java
public class CouponController {
//--- 생략 ...
    public ApiResponse<CompensateCouponResponse> compensateCoupon(@RequestBody CompensateCouponRequest request) {
        compensateCouponUseCase.compensateCoupon(request.couponNumber(), request.orderId());
        return ApiResponse.success(buildCompensateResponse(request.couponNumber(), CouponStatus.AVAILABLE));
    }
//--- 생략 ...
}
```
- 설명: 실패 시 보상 트랜잭션을 수행해 상태를 원복한다.

### 605354d coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 주요 변경: coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 핵심 코드: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/GlobalExceptionHandler.java`
```java
public class GlobalExceptionHandler {
//--- 생략 ...
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(IllegalArgumentException ex) {
        return responseEntityWithHttpStatus(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }
//--- 생략 ...
}
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.

### 7d9e662 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 주요 변경: 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 핵심 코드: `bin_k8s/07_run_local_consumer.sh`
```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

//--- 생략 ...
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.
## 1) OrderSagaConsumer 모듈 추가 및 기본 컨슈머 구성
- 새 모듈 `order-saga-consumer` 추가(기존 `OrderSagaConsumer`에서 이름 변경).
//--- 생략 ...
```
- 설명: Kafka 컨슈머가 이벤트를 수신해 confirm/compensate 흐름을 이어간다.

### 35b85e3 API 응답 에러 명시적으로 변경 중
- 주요 변경: API 응답 에러 명시적으로 변경 중
- 핵심 코드: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`
```java
public class CouponController {
//--- 생략 ...
    public ApiResponse<CompensateCouponResponse> compensateCoupon(@RequestBody CompensateCouponRequest request) {
        compensateCouponUseCase.compensateCoupon(request.couponNumber(), request.orderId());
        return ApiResponse.success(buildCompensateResponse(request.couponNumber(), CouponStatus.AVAILABLE));
    }
//--- 생략 ...
}
```
- 설명: 실패 시 보상 트랜잭션을 수행해 상태를 원복한다.

### 605354d coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 주요 변경: coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 핵심 코드: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/GlobalExceptionHandler.java`
```java
public class GlobalExceptionHandler {
//--- 생략 ...
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(IllegalArgumentException ex) {
        return responseEntityWithHttpStatus(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }
//--- 생략 ...
}
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.

### 7d9e662 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 주요 변경: 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 핵심 코드: `bin_k8s/07_run_local_consumer.sh`
```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

//--- 생략 ...
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.
## 1) OrderSagaConsumer 모듈 추가 및 기본 컨슈머 구성
- 새 모듈 `order-saga-consumer` 추가(기존 `OrderSagaConsumer`에서 이름 변경).
- `OSC_application.yaml` 구성: `order-orchestrator`의 `orderOS_application.yaml` 내용을 복사하고 `sql.init` 제거.
- Kafka 컨슈머 생성: 토픽 메시지 payload 출력 포맷을 `KafkaTopicPrinter` 스타일로 출력.
- `OrderSagaConsumerApplication` 실행 시 `spring.profiles.active` 미지정이면 `test`가 기본이 되도록 설정.
- 실행 스크립트 추가: `bin_k8s/07_dev_consumer.sh`, `bin_k8s/07_test_consumer.sh`.
//--- 생략 ...
```
- 설명: Kafka 컨슈머가 이벤트를 수신해 confirm/compensate 흐름을 이어간다.

### 35b85e3 API 응답 에러 명시적으로 변경 중
- 주요 변경: API 응답 에러 명시적으로 변경 중
- 핵심 코드: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`
```java
public class CouponController {
//--- 생략 ...
    public ApiResponse<ReserveCouponResponse> reserveCoupon(@RequestBody ReserveCouponRequest request) {
        reserveCouponUseCase.reserve(request.couponNumber(), request.orderId());

        return ApiResponse.success(buildReserveResponse(request.couponNumber(), CouponStatus.RESERVED));
    }
//--- 생략 ...
}
```
- 설명: 오케스트레이터가 쿠폰/포인트 예약을 병렬 호출해 분산 트랜잭션의 시작점을 만든다.

### 605354d coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 주요 변경: coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 핵심 코드: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/GlobalExceptionHandler.java`
```java
public class GlobalExceptionHandler {
//--- 생략 ...
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(IllegalArgumentException ex) {
        return responseEntityWithHttpStatus(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }
//--- 생략 ...
}
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.

### 7d9e662 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 주요 변경: 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 핵심 코드: `bin_k8s/07_run_local_consumer.sh`
```bash
//--- 생략 ...
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

"${ROOT_DIR}/gradlew" :order-saga-consumer:bootRun \
  -Dspring.profiles.active=test
```
- 설명: Kafka 컨슈머가 이벤트를 수신해 confirm/compensate 흐름을 이어간다.

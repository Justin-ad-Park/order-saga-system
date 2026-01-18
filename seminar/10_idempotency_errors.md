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

## 코드 발췌 및 설명
- `coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponService.java`: confirm 멱등성 처리(이미 USED면 no-op)
```java
    @Override
    public void confirm(String couponNumber, String orderId) {
        Coupon coupon = loadCouponPort.loadCoupon(couponNumber)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다: " + couponNumber));
        if (coupon.status() == CouponStatus.USED) {
            return;
        }
        validateConfirmable(coupon);

        Coupon updated = new Coupon(
                coupon.couponNumber(),
                CouponStatus.USED,
                coupon.issuedAt(),
                coupon.expiredAt()
        );
        saveCouponPort.save(updated);
    }
```
- 왜 필요한가: 멱등 처리 지점을 보여줘, 중복 호출에서 안정적인 동작 이유를 설명할 수 있다.

## 커밋 상세
### 542ed97 ### Coupon-service confirm API 추가 ###
- 변경 요약: ### Coupon-service confirm API 추가 ###
- 핵심 로직: 예약/확정/보상 API
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`, `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/dto/request/ConfirmCouponRequest.java`
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`
```diff
+import com.example.couponservice.adapter.in.web.dto.request.ConfirmCouponRequest;
+import com.example.couponservice.adapter.in.web.dto.response.ConfirmCouponResponse;
+import com.example.couponservice.application.port.in.ConfirmCouponUseCase;
+    private final ConfirmCouponUseCase confirmCouponUseCase;
+        return ApiResponse.success(buildReserveResponse(request.couponNumber(), CouponStatus.RESERVED));
+    }
+
+    @PostMapping("/confirm")
```
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/dto/request/ConfirmCouponRequest.java`
```diff
+package com.example.couponservice.adapter.in.web.dto.request;
+
+public record ConfirmCouponRequest(
+        String couponNumber,
+        String orderId
+) {
+}
```

### 091c2a7 coupon-service에 보상(compensateCoupon) API 추가
- 변경 요약: coupon-service에 보상(compensateCoupon) API 추가
- 핵심 로직: 예약/확정/보상 API
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`, `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/dto/request/CompensateCouponRequest.java`
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`
```diff
+import com.example.couponservice.adapter.in.web.dto.request.CompensateCouponRequest;
+import com.example.couponservice.adapter.in.web.dto.response.CompensateCouponResponse;
+import com.example.couponservice.application.port.in.CompensateCouponUseCase;
+    private final CompensateCouponUseCase compensateCouponUseCase;
+    @PostMapping("/compensate")
+    public ApiResponse<CompensateCouponResponse> compensateCoupon(@RequestBody CompensateCouponRequest request) {
+        compensateCouponUseCase.compensateCoupon(request.couponNumber(), request.orderId());
+        return ApiResponse.success(buildCompensateResponse(request.couponNumber(), CouponStatus.COMPENSATED));
```
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/dto/request/CompensateCouponRequest.java`
```diff
+package com.example.couponservice.adapter.in.web.dto.request;
+
+public record CompensateCouponRequest(
+        String couponNumber,
+        String orderId
+) {
+}
```

### 66c93ca confirm, compansate API를 point-service에도 동일한 방식으로 추가
- 변경 요약: confirm, compansate API를 point-service에도 동일한 방식으로 추가
- 핵심 로직: 예약/확정/보상 API
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `point-service/src/main/java/com/example/pointservice/adapter/in/web/PointController.java`, `point-service/src/main/java/com/example/pointservice/adapter/in/web/dto/request/CompensatePointRequest.java`
- 코드 발췌: `point-service/src/main/java/com/example/pointservice/adapter/in/web/PointController.java`
```diff
+import com.example.pointservice.adapter.in.web.dto.request.CompensatePointRequest;
+import com.example.pointservice.adapter.in.web.dto.request.ConfirmPointRequest;
+import com.example.pointservice.adapter.in.web.dto.response.CompensatePointResponse;
+import com.example.pointservice.adapter.in.web.dto.response.ConfirmPointResponse;
+import com.example.pointservice.application.port.in.CompensatePointUseCase;
+import com.example.pointservice.application.port.in.ConfirmPointUseCase;
+    private final ConfirmPointUseCase confirmPointUseCase;
+    private final CompensatePointUseCase compensatePointUseCase;
```
- 코드 발췌: `point-service/src/main/java/com/example/pointservice/adapter/in/web/dto/request/CompensatePointRequest.java`
```diff
+package com.example.pointservice.adapter.in.web.dto.request;
+
+public record CompensatePointRequest(
+        String pointNumber,
+        String orderId
+) {
+}
```

### 35b85e3 API 응답 에러 명시적으로 변경 중
- 변경 요약: API 응답 에러 명시적으로 변경 중
- 핵심 로직: API 엔드포인트 처리 흐름
- 구조 변화: 쿠폰/포인트 서비스의 계약 또는 테스트 동시 확장
- 주요 파일: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`, `coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponService.java`
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`
```diff
+        return ApiResponse.success(buildCompensateResponse(request.couponNumber(), CouponStatus.AVAILABLE));
```
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponService.java`
```diff
+        if (coupon == null) {
+            return;
+        }
+        if (coupon.status() == CouponStatus.USED) {
+            throw new IllegalStateException("보상 불가능한 쿠폰입니다: " + coupon.couponNumber());
+        }
+        if (coupon.status() != CouponStatus.RESERVED) {
+                CouponStatus.AVAILABLE,
```

### 605354d coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 변경 요약: coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 핵심 로직: API 엔드포인트 처리 흐름
- 구조 변화: 모듈 또는 테스트 구조 변경
- 주요 파일: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/GlobalExceptionHandler.java`, `coupon-service/src/test/java/com/example/couponservice/adapter/in/web/CouponControllerIntegrationTest.java`
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/GlobalExceptionHandler.java`
```diff
+package com.example.couponservice.adapter.in.web;
+
+import com.example.common.api.ApiError;
+import com.example.common.api.ApiResponse;
+import org.springframework.http.HttpStatus;
+import org.springframework.http.ResponseEntity;
+import org.springframework.web.bind.annotation.ExceptionHandler;
+import org.springframework.web.bind.annotation.RestControllerAdvice;
```
- 코드 발췌: `coupon-service/src/test/java/com/example/couponservice/adapter/in/web/CouponControllerIntegrationTest.java`
```diff
+        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
```

### 7d9e662 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 변경 요약: 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 핵심 로직: 비즈니스 서비스 로직(예약/확정/보상)
- 구조 변화: 소비자 모듈 또는 이벤트 처리 흐름 확장
- 주요 파일: `coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponService.java`, `coupon-service/src/test/java/com/example/couponservice/application/service/ReserveCouponServiceMockTest.java`
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponService.java`
```diff
+        Coupon coupon = loadCouponPort.loadCoupon(couponNumber)
+                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다: " + couponNumber));
+        if (coupon.status() == CouponStatus.USED) {
+            return;
+        }
+        validateConfirmable(coupon);
+
+        Coupon updated = new Coupon(
```
- 코드 발췌: `coupon-service/src/test/java/com/example/couponservice/application/service/ReserveCouponServiceMockTest.java`
```diff
+    @Test
+    void confirm_shouldNoOp_ifCouponAlreadyUsed() {
+        String couponNumber = "CPN-UNIT-USED-001";
+        LocalDateTime now = LocalDateTime.now();
+        Coupon used = new Coupon(couponNumber, CouponStatus.USED, now.minusDays(1), now.plusDays(1));
+
+        when(loadCouponPort.loadCoupon(couponNumber)).thenReturn(Optional.of(used));
```

### 542ed97 ### Coupon-service confirm API 추가 ###
- 변경 요약: ### Coupon-service confirm API 추가 ###
- 핵심 로직: 예약/확정/보상 API
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`, `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/dto/request/ConfirmCouponRequest.java`
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`
```diff
+import com.example.couponservice.adapter.in.web.dto.request.ConfirmCouponRequest;
+import com.example.couponservice.adapter.in.web.dto.response.ConfirmCouponResponse;
+import com.example.couponservice.application.port.in.ConfirmCouponUseCase;
+    private final ConfirmCouponUseCase confirmCouponUseCase;
+        return ApiResponse.success(buildReserveResponse(request.couponNumber(), CouponStatus.RESERVED));
+    }
+
+    @PostMapping("/confirm")
```
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/dto/request/ConfirmCouponRequest.java`
```diff
+package com.example.couponservice.adapter.in.web.dto.request;
+
+public record ConfirmCouponRequest(
+        String couponNumber,
+        String orderId
+) {
+}
```

### 091c2a7 coupon-service에 보상(compensateCoupon) API 추가
- 변경 요약: coupon-service에 보상(compensateCoupon) API 추가
- 핵심 로직: 예약/확정/보상 API
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`, `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/dto/request/CompensateCouponRequest.java`
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`
```diff
+import com.example.couponservice.adapter.in.web.dto.request.CompensateCouponRequest;
+import com.example.couponservice.adapter.in.web.dto.response.CompensateCouponResponse;
+import com.example.couponservice.application.port.in.CompensateCouponUseCase;
+    private final CompensateCouponUseCase compensateCouponUseCase;
+    @PostMapping("/compensate")
+    public ApiResponse<CompensateCouponResponse> compensateCoupon(@RequestBody CompensateCouponRequest request) {
+        compensateCouponUseCase.compensateCoupon(request.couponNumber(), request.orderId());
+        return ApiResponse.success(buildCompensateResponse(request.couponNumber(), CouponStatus.COMPENSATED));
```
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/dto/request/CompensateCouponRequest.java`
```diff
+package com.example.couponservice.adapter.in.web.dto.request;
+
+public record CompensateCouponRequest(
+        String couponNumber,
+        String orderId
+) {
+}
```

### 66c93ca confirm, compansate API를 point-service에도 동일한 방식으로 추가
- 변경 요약: confirm, compansate API를 point-service에도 동일한 방식으로 추가
- 핵심 로직: 예약/확정/보상 API
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `point-service/src/main/java/com/example/pointservice/adapter/in/web/PointController.java`, `point-service/src/main/java/com/example/pointservice/adapter/in/web/dto/request/CompensatePointRequest.java`
- 코드 발췌: `point-service/src/main/java/com/example/pointservice/adapter/in/web/PointController.java`
```diff
+import com.example.pointservice.adapter.in.web.dto.request.CompensatePointRequest;
+import com.example.pointservice.adapter.in.web.dto.request.ConfirmPointRequest;
+import com.example.pointservice.adapter.in.web.dto.response.CompensatePointResponse;
+import com.example.pointservice.adapter.in.web.dto.response.ConfirmPointResponse;
+import com.example.pointservice.application.port.in.CompensatePointUseCase;
+import com.example.pointservice.application.port.in.ConfirmPointUseCase;
+    private final ConfirmPointUseCase confirmPointUseCase;
+    private final CompensatePointUseCase compensatePointUseCase;
```
- 코드 발췌: `point-service/src/main/java/com/example/pointservice/adapter/in/web/dto/request/CompensatePointRequest.java`
```diff
+package com.example.pointservice.adapter.in.web.dto.request;
+
+public record CompensatePointRequest(
+        String pointNumber,
+        String orderId
+) {
+}
```

### 35b85e3 API 응답 에러 명시적으로 변경 중
- 변경 요약: API 응답 에러 명시적으로 변경 중
- 핵심 로직: API 엔드포인트 처리 흐름
- 구조 변화: 쿠폰/포인트 서비스의 계약 또는 테스트 동시 확장
- 주요 파일: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`, `coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponService.java`
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/CouponController.java`
```diff
+        return ApiResponse.success(buildCompensateResponse(request.couponNumber(), CouponStatus.AVAILABLE));
```
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponService.java`
```diff
+        if (coupon == null) {
+            return;
+        }
+        if (coupon.status() == CouponStatus.USED) {
+            throw new IllegalStateException("보상 불가능한 쿠폰입니다: " + coupon.couponNumber());
+        }
+        if (coupon.status() != CouponStatus.RESERVED) {
+                CouponStatus.AVAILABLE,
```

### 605354d coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 변경 요약: coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장
- 핵심 로직: API 엔드포인트 처리 흐름
- 구조 변화: 모듈 또는 테스트 구조 변경
- 주요 파일: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/GlobalExceptionHandler.java`, `coupon-service/src/test/java/com/example/couponservice/adapter/in/web/CouponControllerIntegrationTest.java`
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/adapter/in/web/GlobalExceptionHandler.java`
```diff
+package com.example.couponservice.adapter.in.web;
+
+import com.example.common.api.ApiError;
+import com.example.common.api.ApiResponse;
+import org.springframework.http.HttpStatus;
+import org.springframework.http.ResponseEntity;
+import org.springframework.web.bind.annotation.ExceptionHandler;
+import org.springframework.web.bind.annotation.RestControllerAdvice;
```
- 코드 발췌: `coupon-service/src/test/java/com/example/couponservice/adapter/in/web/CouponControllerIntegrationTest.java`
```diff
+        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
```

### 7d9e662 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 변경 요약: 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성)
- 핵심 로직: 비즈니스 서비스 로직(예약/확정/보상)
- 구조 변화: 소비자 모듈 또는 이벤트 처리 흐름 확장
- 주요 파일: `coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponService.java`, `coupon-service/src/test/java/com/example/couponservice/application/service/ReserveCouponServiceMockTest.java`
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponService.java`
```diff
+        Coupon coupon = loadCouponPort.loadCoupon(couponNumber)
+                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다: " + couponNumber));
+        if (coupon.status() == CouponStatus.USED) {
+            return;
+        }
+        validateConfirmable(coupon);
+
+        Coupon updated = new Coupon(
```
- 코드 발췌: `coupon-service/src/test/java/com/example/couponservice/application/service/ReserveCouponServiceMockTest.java`
```diff
+    @Test
+    void confirm_shouldNoOp_ifCouponAlreadyUsed() {
+        String couponNumber = "CPN-UNIT-USED-001";
+        LocalDateTime now = LocalDateTime.now();
+        Coupon used = new Coupon(couponNumber, CouponStatus.USED, now.minusDays(1), now.plusDays(1));
+
+        when(loadCouponPort.loadCoupon(couponNumber)).thenReturn(Optional.of(used));
```

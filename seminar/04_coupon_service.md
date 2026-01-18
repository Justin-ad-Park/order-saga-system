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

## 코드 발췌 및 설명
- `coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponService.java`: 쿠폰 예약/보상 흐름 핵심 로직
```java
    @Override
    public void reserve(String couponNumber, String orderId) {
        if (isReservationCancelled(orderId)) {
            return;
        }
        verifyReservationNotAlreadyReserved(orderId);
        updateStatus(couponNumber, CouponStatus.RESERVED, this::validateReservable);
        saveCouponReservationPort.saveReservation(new CouponReservation(
                orderId,
                couponNumber,
                ReservationStatus.RESERVED
        ));
    }

    @Override
    public void compensateCoupon(String couponNumber, String orderId) {
        Coupon coupon = loadCouponPort.loadCoupon(couponNumber)
                .orElse(null);
        if (coupon == null) {
            saveReservationCancelled(orderId, couponNumber);
            return;
        }
        if (coupon.status() == CouponStatus.USED) {
            throw new IllegalStateException("보상 불가능한 쿠폰입니다: " + coupon.couponNumber());
        }

        saveReservationCancelled(orderId, couponNumber);
        if (coupon.status() != CouponStatus.RESERVED) {
            return;
        }

        Coupon updated = new Coupon(
                coupon.couponNumber(),
                CouponStatus.AVAILABLE,
                coupon.issuedAt(),
                coupon.expiredAt()
        );
        saveCouponPort.save(updated);
    }
```
- 왜 필요한가: 예약/보상 전이 핵심을 보여줘, 실패 시 보상 흐름의 출발점을 이해시키기 좋다.

## 커밋 상세
### 79dec4c [Coupon-service]First commit
- 변경 요약: [Coupon-service]First commit
- 핵심 로직: 모듈/의존성 구성 변경
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `coupon-service/build.gradle`, `coupon-service/src/main/java/com/example/couponservice/CouponServiceApplication.java`
- 코드 발췌: `coupon-service/build.gradle`
```diff
+plugins {
+    id 'org.springframework.boot'
+    id 'io.spring.dependency-management' version '1.1.5'
+    id 'java'
+}
+
+dependencies {
+    // Web API (기존 ver08과 동일하게 MVC 기반으로 시작)
```
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/CouponServiceApplication.java`
```diff
+package com.example.couponservice;
+
+import org.springframework.boot.SpringApplication;
+import org.springframework.boot.autoconfigure.SpringBootApplication;
+
+
+@SpringBootApplication
+public class CouponServiceApplication {
```

### 3103fe4 ReserveCouponServiceTest Mock
- 변경 요약: ReserveCouponServiceTest Mock
- 핵심 로직: 예약/확정/보상 API
- 구조 변화: 모듈 또는 테스트 구조 변경
- 주요 파일: `coupon-service/src/test/java/com/example/couponservice/application/service/ReserveCouponServiceTest.java`
- 코드 발췌: `coupon-service/src/test/java/com/example/couponservice/application/service/ReserveCouponServiceTest.java`
```diff
+package com.example.couponservice.application.service;
+
+import com.example.couponservice.application.port.out.LoadCouponPort;
+import com.example.couponservice.application.port.out.SaveCouponPort;
+import com.example.couponservice.domain.model.Coupon;
+import com.example.couponservice.domain.model.status.CouponStatus;
+import org.junit.jupiter.api.BeforeEach;
+import org.junit.jupiter.api.Test;
```

### db4881a Coupon-service 연계 통합 테스트
- 변경 요약: Coupon-service 연계 통합 테스트
- 핵심 로직: 테스트 케이스 확장
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `coupon-service/src/main/resources/application.yaml`, `coupon-service/src/main/resources/schema.sql`
- 코드 발췌: `coupon-service/src/main/resources/application.yaml`
```diff
+  jpa:
+
+  sql:
+    init:
+      mode: always
+      schema-locations: classpath:schema.sql
+      enabled: true  # http://localhost:8081/h2-console
+  type: h2
```
- 코드 발췌: `coupon-service/src/main/resources/schema.sql`
```diff
+CREATE TABLE IF NOT EXISTS coupon (
+    coupon_number VARCHAR(255) PRIMARY KEY,
+    status VARCHAR(255) NOT NULL,
+    issued_at TIMESTAMP NOT NULL,
+    expired_at TIMESTAMP NOT NULL
+);
+
+MERGE INTO coupon (coupon_number, status, issued_at, expired_at)
```

### 95df8c2 통합 테스트 개선
- 변경 요약: 통합 테스트 개선
- 핵심 로직: 테스트 케이스 확장
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `coupon-service/src/main/java/com/example/couponservice/config/TestDataCleaner.java`, `coupon-service/src/main/resources/application.yaml`
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/config/TestDataCleaner.java`
```diff
+package com.example.couponservice.config;
+
+import com.example.couponservice.adapter.out.persistence.jpa.CouponJpaRepository;
+import org.springframework.boot.ApplicationArguments;
+import org.springframework.boot.ApplicationRunner;
+import org.springframework.context.annotation.Profile;
+import org.springframework.stereotype.Component;
```
- 코드 발췌: `coupon-service/src/main/resources/application.yaml`
```diff
+    active: test
+      on-profile: test
+    defer-datasource-initialization: true
```

### 58d7578 schema.sql 실행 이슈 관련 테스트 오류 수정
- 변경 요약: schema.sql 실행 이슈 관련 테스트 오류 수정
- 핵심 로직: DB 스키마/테스트 데이터
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `coupon-service/src/main/resources/application.yaml`, `coupon-service/src/main/resources/schema.sql`
- 코드 발췌: `coupon-service/src/main/resources/application.yaml`
```diff
+#spring:
+#  profiles:
+#    active: test
+
+---
+
+---
+spring:
```
- 코드 발췌: `coupon-service/src/main/resources/schema.sql`
```diff
+MERGE INTO coupon (coupon_number, status, issued_at, expired_at)
+    KEY(coupon_number)
+    VALUES ('C-001', 'AVAILABLE', CURRENT_TIMESTAMP, DATEADD('DAY', 30, CURRENT_TIMESTAMP));
+
```

### 79dec4c [Coupon-service]First commit
- 변경 요약: [Coupon-service]First commit
- 핵심 로직: 모듈/의존성 구성 변경
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `coupon-service/build.gradle`, `coupon-service/src/main/java/com/example/couponservice/CouponServiceApplication.java`
- 코드 발췌: `coupon-service/build.gradle`
```diff
+plugins {
+    id 'org.springframework.boot'
+    id 'io.spring.dependency-management' version '1.1.5'
+    id 'java'
+}
+
+dependencies {
+    // Web API (기존 ver08과 동일하게 MVC 기반으로 시작)
```
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/CouponServiceApplication.java`
```diff
+package com.example.couponservice;
+
+import org.springframework.boot.SpringApplication;
+import org.springframework.boot.autoconfigure.SpringBootApplication;
+
+
+@SpringBootApplication
+public class CouponServiceApplication {
```

### 3103fe4 ReserveCouponServiceTest Mock
- 변경 요약: ReserveCouponServiceTest Mock
- 핵심 로직: 예약/확정/보상 API
- 구조 변화: 모듈 또는 테스트 구조 변경
- 주요 파일: `coupon-service/src/test/java/com/example/couponservice/application/service/ReserveCouponServiceTest.java`
- 코드 발췌: `coupon-service/src/test/java/com/example/couponservice/application/service/ReserveCouponServiceTest.java`
```diff
+package com.example.couponservice.application.service;
+
+import com.example.couponservice.application.port.out.LoadCouponPort;
+import com.example.couponservice.application.port.out.SaveCouponPort;
+import com.example.couponservice.domain.model.Coupon;
+import com.example.couponservice.domain.model.status.CouponStatus;
+import org.junit.jupiter.api.BeforeEach;
+import org.junit.jupiter.api.Test;
```

### db4881a Coupon-service 연계 통합 테스트
- 변경 요약: Coupon-service 연계 통합 테스트
- 핵심 로직: 테스트 케이스 확장
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `coupon-service/src/main/resources/application.yaml`, `coupon-service/src/main/resources/schema.sql`
- 코드 발췌: `coupon-service/src/main/resources/application.yaml`
```diff
+  jpa:
+
+  sql:
+    init:
+      mode: always
+      schema-locations: classpath:schema.sql
+      enabled: true  # http://localhost:8081/h2-console
+  type: h2
```
- 코드 발췌: `coupon-service/src/main/resources/schema.sql`
```diff
+CREATE TABLE IF NOT EXISTS coupon (
+    coupon_number VARCHAR(255) PRIMARY KEY,
+    status VARCHAR(255) NOT NULL,
+    issued_at TIMESTAMP NOT NULL,
+    expired_at TIMESTAMP NOT NULL
+);
+
+MERGE INTO coupon (coupon_number, status, issued_at, expired_at)
```

### 95df8c2 통합 테스트 개선
- 변경 요약: 통합 테스트 개선
- 핵심 로직: 테스트 케이스 확장
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `coupon-service/src/main/java/com/example/couponservice/config/TestDataCleaner.java`, `coupon-service/src/main/resources/application.yaml`
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/config/TestDataCleaner.java`
```diff
+package com.example.couponservice.config;
+
+import com.example.couponservice.adapter.out.persistence.jpa.CouponJpaRepository;
+import org.springframework.boot.ApplicationArguments;
+import org.springframework.boot.ApplicationRunner;
+import org.springframework.context.annotation.Profile;
+import org.springframework.stereotype.Component;
```
- 코드 발췌: `coupon-service/src/main/resources/application.yaml`
```diff
+    active: test
+      on-profile: test
+    defer-datasource-initialization: true
```

### 58d7578 schema.sql 실행 이슈 관련 테스트 오류 수정
- 변경 요약: schema.sql 실행 이슈 관련 테스트 오류 수정
- 핵심 로직: DB 스키마/테스트 데이터
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `coupon-service/src/main/resources/application.yaml`, `coupon-service/src/main/resources/schema.sql`
- 코드 발췌: `coupon-service/src/main/resources/application.yaml`
```diff
+#spring:
+#  profiles:
+#    active: test
+
+---
+
+---
+spring:
```
- 코드 발췌: `coupon-service/src/main/resources/schema.sql`
```diff
+MERGE INTO coupon (coupon_number, status, issued_at, expired_at)
+    KEY(coupon_number)
+    VALUES ('C-001', 'AVAILABLE', CURRENT_TIMESTAMP, DATEADD('DAY', 30, CURRENT_TIMESTAMP));
+
```

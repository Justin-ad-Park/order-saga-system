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

## 코드 발췌 및 설명
- `settings.gradle`: MSA 모듈 구성을 한눈에 보여주는 설정 파일
```gradle
rootProject.name = 'order-saga-system'

include 'order-orchestrator'
include 'order-saga-consumer'
include 'account'
include 'common'
include 'coupon-service'
include 'point-service'
```
- 왜 필요한가: MSA 경계를 명확히 보여줘 전체 구성(오케스트레이터/쿠폰/포인트/컨슈머)이 어떻게 분리되는지 한눈에 설명할 수 있다.

## 커밋 상세
### a080f1d order start
- 변경 요약: order start
- 핵심 로직: 모듈/의존성 구성 변경
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `build.gradle`, `order-orchestrator/build.gradle`
- 코드 발췌: `build.gradle`
```diff
+plugins {
+    id 'org.springframework.boot' version '3.3.2' apply false
+    id 'io.spring.dependency-management' version '1.1.5' apply false
+    id 'java'
+}
+
+allprojects {
+    group = 'com.example.order'
```
- 코드 발췌: `order-orchestrator/build.gradle`
```diff
+plugins {
+    id 'org.springframework.boot'
+    id 'io.spring.dependency-management'
+    id 'java'
+}
+
+dependencies {
+    // Web API (기존 ver08과 동일하게 MVC 기반으로 시작)
```

### 82e897a 주문 오케스트레이터의 기본 골격만 완성. Persistent 개발 안됨
- 변경 요약: 주문 오케스트레이터의 기본 골격만 완성. Persistent 개발 안됨
- 핵심 로직: 모듈/의존성 구성 변경
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `account/build.gradle`, `account/settings.gradle`
- 코드 발췌: `account/build.gradle`
```diff
+plugins {
+    id 'java'
+    id 'org.springframework.boot' version '3.3.2'
+    id 'io.spring.dependency-management' version '1.1.5'
+}
+
+group = 'com.example'
+version = '1.0.0'
```
- 코드 발췌: `account/settings.gradle`
```diff
+rootProject.name = 'account'
```

### e37883c ### Common 모듈 추가 ######################
- 변경 요약: ### Common 모듈 추가 ######################
- 핵심 로직: 모듈/의존성 구성 변경
- 구조 변화: 공통 모듈 추가 및 의존성 정리
- 주요 파일: `common/build.gradle`, `common/src/main/java/com/example/common/api/ApiError.java`
- 코드 발췌: `common/build.gradle`
```diff
+// org.springframework.boot 플러그인 절대 적용하지 않기
+plugins {
+    id 'java-library'
+}
+
+group = 'com.example'
+version = '0.0.1-SNAPSHOT'
```
- 코드 발췌: `common/src/main/java/com/example/common/api/ApiError.java`
```diff
+package com.example.common.api;
+
+// 공통 에러 DTO (web 계층)
+public class ApiError {
+    private final String code;
+    private final String message;
+    private ApiError(String code, String message) { this.code = code; this.message = message; }
+    public String getCode() { return code; }
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

### 193e5e2 First commit for Point MSA
- 변경 요약: First commit for Point MSA
- 핵심 로직: 모듈/의존성 구성 변경
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `order-orchestrator/build.gradle`, `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`
- 코드 발췌: `order-orchestrator/build.gradle`
```diff
+    testImplementation project(':point-service')
```
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`
```diff
+import com.example.orderorchestrator.adapter.out.webclient.PointServiceClient;
+    private final PointServiceClient pointServiceClient;
+        return Mono.when(
+                        couponServiceClient.reserveCoupon(request.couponNumber(), result.orderId()),
+                        pointServiceClient.reservePoint(request.pointNumber(), result.orderId())
+                )
```

### a080f1d order start
- 변경 요약: order start
- 핵심 로직: 모듈/의존성 구성 변경
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `build.gradle`, `order-orchestrator/build.gradle`
- 코드 발췌: `build.gradle`
```diff
+plugins {
+    id 'org.springframework.boot' version '3.3.2' apply false
+    id 'io.spring.dependency-management' version '1.1.5' apply false
+    id 'java'
+}
+
+allprojects {
+    group = 'com.example.order'
```
- 코드 발췌: `order-orchestrator/build.gradle`
```diff
+plugins {
+    id 'org.springframework.boot'
+    id 'io.spring.dependency-management'
+    id 'java'
+}
+
+dependencies {
+    // Web API (기존 ver08과 동일하게 MVC 기반으로 시작)
```

### 82e897a 주문 오케스트레이터의 기본 골격만 완성. Persistent 개발 안됨
- 변경 요약: 주문 오케스트레이터의 기본 골격만 완성. Persistent 개발 안됨
- 핵심 로직: 모듈/의존성 구성 변경
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `account/build.gradle`, `account/settings.gradle`
- 코드 발췌: `account/build.gradle`
```diff
+plugins {
+    id 'java'
+    id 'org.springframework.boot' version '3.3.2'
+    id 'io.spring.dependency-management' version '1.1.5'
+}
+
+group = 'com.example'
+version = '1.0.0'
```
- 코드 발췌: `account/settings.gradle`
```diff
+rootProject.name = 'account'
```

### e37883c ### Common 모듈 추가 ######################
- 변경 요약: ### Common 모듈 추가 ######################
- 핵심 로직: 모듈/의존성 구성 변경
- 구조 변화: 공통 모듈 추가 및 의존성 정리
- 주요 파일: `common/build.gradle`, `common/src/main/java/com/example/common/api/ApiError.java`
- 코드 발췌: `common/build.gradle`
```diff
+// org.springframework.boot 플러그인 절대 적용하지 않기
+plugins {
+    id 'java-library'
+}
+
+group = 'com.example'
+version = '0.0.1-SNAPSHOT'
```
- 코드 발췌: `common/src/main/java/com/example/common/api/ApiError.java`
```diff
+package com.example.common.api;
+
+// 공통 에러 DTO (web 계층)
+public class ApiError {
+    private final String code;
+    private final String message;
+    private ApiError(String code, String message) { this.code = code; this.message = message; }
+    public String getCode() { return code; }
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

### 193e5e2 First commit for Point MSA
- 변경 요약: First commit for Point MSA
- 핵심 로직: 모듈/의존성 구성 변경
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `order-orchestrator/build.gradle`, `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`
- 코드 발췌: `order-orchestrator/build.gradle`
```diff
+    testImplementation project(':point-service')
```
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`
```diff
+import com.example.orderorchestrator.adapter.out.webclient.PointServiceClient;
+    private final PointServiceClient pointServiceClient;
+        return Mono.when(
+                        couponServiceClient.reserveCoupon(request.couponNumber(), result.orderId()),
+                        pointServiceClient.reservePoint(request.pointNumber(), result.orderId())
+                )
```

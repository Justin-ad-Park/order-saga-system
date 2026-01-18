# 05. 포인트 서비스 구축과 MSA 확장

## 목표
- 포인트 서비스 추가로 MSA 구성이 확장되는 과정을 이해한다.

## 스토리라인
- 쿠폰 서비스 패턴을 포인트로 확장하면서 중복과 재사용 포인트를 찾음.

## 관련 커밋
- `193e5e2`, `6bb3683`, `34f3209`

## 커밋 변경 요약
| 커밋 | 주요 변경 요약 | 체크아웃 |
| --- | --- | --- |
| `193e5e2` | First commit for Point MSA | `git checkout 193e5e2` |
| `6bb3683` | point-service, coupon-service 테스트 케이스 추가 | `git checkout 6bb3683` |
| `34f3209` | 통합 테스트 확장 | `git checkout 34f3209` |

## 핵심 개념
- 비슷한 서비스 간 계약 일관성
- 테스트 케이스 확장

## 기술/기능/프로세스
- 기술: Spring Boot, JPA, MySQL, REST
- 기능: 포인트 reserve/confirm/compensate
- MSA: 포인트 서비스 독립 배포
- EDA: 쿠폰과 동일한 계약으로 이벤트 흐름에 참여
## 데모/실습
- 테스트 데이터 확인: `point-service/src/main/resources/point_schema.sql`
- 통합 테스트: `point-service/src/test/java/.../PointControllerIntegrationTest.java`

## 코드 발췌 및 설명
- `point-service/src/main/java/com/example/pointservice/application/service/ReservePointService.java`: 포인트 예약/확정/보상 로직
```java
    @Override
    public void reserve(String pointNumber, String orderId) {
        if (isReservationCancelled(orderId)) {
            return;
        }
        verifyReservationNotAlreadyReserved(orderId);
        updateStatus(pointNumber, PointStatus.RESERVED, this::validateReservable);
        savePointReservationPort.saveReservation(new PointReservation(
                orderId,
                pointNumber,
                ReservationStatus.RESERVED
        ));
    }

    @Override
    public void confirm(String pointNumber, String orderId) {
        Point point = loadPointPort.loadPoint(pointNumber)
                .orElseThrow(() -> new IllegalArgumentException("포인트를 찾을 수 없습니다: " + pointNumber));
        if (point.status() == PointStatus.USED) {
            return;
        }
        validateConfirmable(point);

        Point updated = new Point(
                point.pointNumber(),
                PointStatus.USED,
                point.issuedAt(),
                point.expiredAt()
        );
        savePointPort.save(updated);
    }
```
- 왜 필요한가: 포인트도 동일한 계약을 갖는다는 점을 보여줘, MSA 확장의 일관성을 설명할 수 있다.

## 커밋 상세
### 193e5e2 First commit for Point MSA
- 변경 요약: First commit for Point MSA
- 핵심 로직: 모듈/의존성 구성 변경
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `point-service/build.gradle`, `point-service/src/main/java/com/example/pointservice/PointServiceApplication.java`
- 코드 발췌: `point-service/build.gradle`
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
- 코드 발췌: `point-service/src/main/java/com/example/pointservice/PointServiceApplication.java`
```diff
+package com.example.pointservice;
+
+import org.springframework.boot.SpringApplication;
+import org.springframework.boot.autoconfigure.SpringBootApplication;
+import org.springframework.boot.builder.SpringApplicationBuilder;
+
+
+@SpringBootApplication
```

### 6bb3683 point-service, coupon-service 테스트 케이스 추가
- 변경 요약: point-service, coupon-service 테스트 케이스 추가
- 핵심 로직: 테스트 케이스 확장
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `point-service/src/main/resources/point_dev_schema.sql`, `point-service/src/main/resources/point_schema.sql`
- 변경 전/후 비교: `point-service/src/main/resources/point_dev_schema.sql`
- diff 스타일
```diff
@@ -54,3 +54,27 @@ VALUES (
                          status = VALUES(status),
                          issued_at = VALUES(issued_at),
                          expired_at = VALUES(expired_at);
+
+INSERT INTO point (point_number, status, issued_at, expired_at)
+VALUES (
+           'PNT-INT-AVAILABLE-001',
+           'AVAILABLE',
+           CURRENT_TIMESTAMP,
+           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
+       )
+    ON DUPLICATE KEY UPDATE
+                         status = VALUES(status),
+                         issued_at = VALUES(issued_at),
+                         expired_at = VALUES(expired_at);
```
- 코드 발췌: `point-service/src/main/resources/point_dev_schema.sql`
```diff
+
+INSERT INTO point (point_number, status, issued_at, expired_at)
+VALUES (
+           'PNT-INT-AVAILABLE-001',
+           'AVAILABLE',
+           CURRENT_TIMESTAMP,
+           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
+       )
```
- 코드 발췌: `point-service/src/main/resources/point_schema.sql`
```diff
+
+INSERT INTO point (point_number, status, issued_at, expired_at)
+VALUES (
+           'PNT-INT-AVAILABLE-001',
+           'AVAILABLE',
+           CURRENT_TIMESTAMP,
+           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
+       )
```

### 34f3209 통합 테스트 확장
- 변경 요약: 통합 테스트 확장
- 핵심 로직: 테스트 케이스 확장
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `order-orchestrator/scripts/run_local_msa.sh`, `order-orchestrator/scripts/stop_local_msa.sh`
- 코드 발췌: `order-orchestrator/scripts/run_local_msa.sh`
```diff
+#!/usr/bin/env bash
+set -euo pipefail
+
+ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
+
+"${ROOT_DIR}/gradlew" :coupon-service:bootRun \
+  -Dspring.profiles.active=test \
+  -Dspring.config.name=coupon_application \
```
- 코드 발췌: `order-orchestrator/scripts/stop_local_msa.sh`
```diff
+#!/usr/bin/env bash
+set -euo pipefail
+
+if command -v lsof >/dev/null 2>&1; then
+  lsof -ti tcp:8080 | xargs kill
+  lsof -ti tcp:8081 | xargs kill
+  lsof -ti tcp:8082 | xargs kill
+else
```

# 13. 타이밍 이슈와 데이터 리셋

## 목표
- 보상 타이밍 이슈를 발견하고 해결하는 과정을 이해한다.

## 스토리라인
- 이벤트 순서/지연이 꼬이면서 보상 타이밍 이슈가 발생.
- 리셋/스냅샷 도구로 반복 검증 환경을 만들고 해결.

## 관련 커밋
- `c4401c7`, `a16fa0c`, `bfa985f`, `1864862`, `eeca8aa`, `c1100d4`

## 커밋 변경 요약
| 커밋 | 주요 변경 요약 | 체크아웃 |
| --- | --- | --- |
| `c4401c7` | Istio 설치. 강제 타임아웃 테스트용 로직 추가, *** 보상 트랜잭션 타이밍 오류 존재함 *** | `git checkout c4401c7` |
| `a16fa0c` | Timing Issue를 잡기 위한 로직 추가 | `git checkout a16fa0c` |
| `bfa985f` | 타이밍 이슈 작업 완료 | `git checkout bfa985f` |
| `1864862` | timing issue 테스트 케이스 추가 | `git checkout 1864862` |
| `eeca8aa` | Saga 반복 테스트 가능하도록 데이터 초기화(OUTBOX_MESSAGE, ORDER_SAGA, ORDER_ITEM) | `git checkout eeca8aa` |
| `c1100d4` | Snapshot 생성 시점을 sh에서 각 서비스 기동 스크립트(*schema.sql)로 변경 | `git checkout c1100d4` |

## 핵심 개념
- 보상 타이밍 이슈 원인 분석
- 스냅샷 기반 리셋 절차

## 기술/기능/프로세스
- 기술: 스냅샷 프로시저, 리셋 스크립트
- 기능: 보상 타이밍 이슈 재현/해결
- MSA: 상태 일관성 유지 전략
- EDA: 이벤트 순서/지연 영향 분석
## 데모/실습
- 리셋: `bin_common/05_reset_test_data.sh`
- 보상 테스트: `bin_istio_test/05_test_saga_compensation.sh`

## 코드 발췌 및 설명
- `bin_common/05_reset_test_data.sh`: 스냅샷 기반 리셋으로 반복 테스트
```bash
kubectl -n msa exec -i deploy/mysql -- \
  mysql -uroot -prootpw -e "CALL order_orchestrator_db.sp_truncate_order_orchestrator_test_data(); CALL coupon_db.sp_reset_coupon_test_data(); CALL point_db.sp_reset_point_test_data();"
```
- 왜 필요한가: 리셋 스크립트가 왜 필요한지 보여줘, 반복 테스트와 타이밍 이슈 재현/해결을 설명할 수 있다.

## 커밋 상세
### c4401c7 Istio 설치. 강제 타임아웃 테스트용 로직 추가, *** 보상 트랜잭션 타이밍 오류 존재함 ***
- 변경 요약: Istio 설치. 강제 타임아웃 테스트용 로직 추가, *** 보상 트랜잭션 타이밍 오류 존재함 ***
- 핵심 로직: 회로 차단/타임아웃 구성
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `bin_common/00_prepare_mysql_kafka.sh`, `bin_common/02_prepare_k8s_order_saga_Local_Consumer.sh`
- 코드 발췌: `bin_common/00_prepare_mysql_kafka.sh`
```diff
+#!/usr/bin/env bash
+set -euo pipefail
+
+COMMON_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
+# shellcheck disable=SC1091
+source "${COMMON_DIR}/lib.sh"
+
+# 1) MySQL/Kafka 포트포워드 정리
```
- 코드 발췌: `bin_common/02_prepare_k8s_order_saga_Local_Consumer.sh`
```diff
+#!/usr/bin/env bash
+set -euo pipefail
+
+COMMON_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
+# shellcheck disable=SC1091
+source "${COMMON_DIR}/lib.sh"
+
+ISTIO_ENABLED="${ISTIO_ENABLED:-false}"
```

### a16fa0c Timing Issue를 잡기 위한 로직 추가
- 변경 요약: Timing Issue를 잡기 위한 로직 추가
- 핵심 로직: 핵심 로직 추가/구조 변경
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `bin_k8s/sql/create_test_snapshots.sql`, `coupon-service/src/main/java/com/example/couponservice/adapter/out/persistence/CouponReservationPersistenceAdapter.java`
- 코드 발췌: `bin_k8s/sql/create_test_snapshots.sql`
```diff
+CREATE TABLE IF NOT EXISTS coupon_reservation_snapshot LIKE coupon_reservation;
+TRUNCATE TABLE coupon_reservation_snapshot;
+INSERT INTO coupon_reservation_snapshot
+SELECT *
+FROM coupon_reservation;
+
+  TRUNCATE TABLE coupon_reservation;
+  INSERT INTO coupon_reservation SELECT * FROM coupon_reservation_snapshot;
```
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/adapter/out/persistence/CouponReservationPersistenceAdapter.java`
```diff
+package com.example.couponservice.adapter.out.persistence;
+
+import com.example.couponservice.adapter.out.persistence.jpa.CouponReservationJpaEntity;
+import com.example.couponservice.adapter.out.persistence.jpa.CouponReservationJpaRepository;
+import com.example.couponservice.application.port.out.LoadCouponReservationPort;
+import com.example.couponservice.application.port.out.SaveCouponReservationPort;
+import com.example.couponservice.domain.model.CouponReservation;
+import com.example.couponservice.domain.model.status.ReservationStatus;
```

### bfa985f 타이밍 이슈 작업 완료
- 변경 요약: 타이밍 이슈 작업 완료
- 핵심 로직: API 엔드포인트 처리 흐름
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`, `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/webclient/CouponServiceClient.java`
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`
```diff
+import com.example.orderorchestrator.application.service.OrderSagaEventService;
+import com.example.orderorchestrator.application.service.ReserveExternalResourcesService;
+    private final ReserveExternalResourcesService reserveExternalResourcesService;
+    private final OrderSagaEventService orderSagaEventService;
+        return reserveExternalResourcesService.reserveExternalResources(
+                        result.orderId(),
+                        request.couponNumber(),
+                        request.pointNumber()
```
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/webclient/CouponServiceClient.java`
```diff
+import com.example.orderorchestrator.adapter.out.webclient.dto.WebApiResponse;
+import com.example.orderorchestrator.application.port.out.ReserveCouponPort;
+public class CouponServiceClient implements ReserveCouponPort {
+    @Override
+    public Mono<Void> reserveCoupon(String couponNumber, String orderId) {
+                .bodyToMono(new ParameterizedTypeReference<WebApiResponse<ReserveCouponResponse>>() {})
+                })
+                .then();
```

### 1864862 timing issue 테스트 케이스 추가
- 변경 요약: timing issue 테스트 케이스 추가
- 핵심 로직: 테스트 케이스 확장
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `bin_k8s/sql/QueryTestData.sql`, `bin_k8s/sql/create_test_snapshots.sql`
- 코드 발췌: `bin_k8s/sql/QueryTestData.sql`
```diff
+SELECT * FROM order_orchestrator_db.OUTBOX_MESSAGE ;
+SELECT * FROM order_orchestrator_db.ORDER_SAGA ;
+SELECT * FROM order_orchestrator_db.ORDER_ITEM ;
+
+SELECT * FROM point_db.point;
+SELECT * FROM point_db.point_reservation;
+SELECT * FROM coupon_db.coupon;
+SELECT * FROM coupon_db.coupon_reservation;
```
- 코드 발췌: `bin_k8s/sql/create_test_snapshots.sql`
```
CREATE DATABASE IF NOT EXISTS coupon_db;
USE coupon_db;

CREATE TABLE IF NOT EXISTS coupon_snapshot LIKE coupon;
TRUNCATE TABLE coupon_snapshot;
INSERT INTO coupon_snapshot
SELECT *
FROM coupon;
```

### eeca8aa Saga 반복 테스트 가능하도록 데이터 초기화(OUTBOX_MESSAGE, ORDER_SAGA, ORDER_ITEM)
- 변경 요약: Saga 반복 테스트 가능하도록 데이터 초기화(OUTBOX_MESSAGE, ORDER_SAGA, ORDER_ITEM)
- 핵심 로직: 테스트 케이스 확장
- 구조 변화: 운영/실행 스크립트 표준화
- 주요 파일: `bin_common/05_reset_test_data.sh`, `bin_k8s/sql/create_test_snapshots.sql`
- 코드 발췌: `bin_common/05_reset_test_data.sh`
```diff
+  mysql -uroot -prootpw -e "CALL order_orchestrator_db.sp_truncate_order_orchestrator_test_data(); CALL coupon_db.sp_reset_coupon_test_data(); CALL point_db.sp_reset_point_test_data();"
```
- 코드 발췌: `bin_k8s/sql/create_test_snapshots.sql`
```diff
+CREATE DATABASE IF NOT EXISTS order_orchestrator_db;
+USE order_orchestrator_db;
+
+DROP PROCEDURE IF EXISTS sp_truncate_order_orchestrator_test_data;
+DELIMITER $$
+CREATE PROCEDURE sp_truncate_order_orchestrator_test_data()
+BEGIN
+  SET FOREIGN_KEY_CHECKS = 0;
```

### c1100d4 Snapshot 생성 시점을 sh에서 각 서비스 기동 스크립트(*schema.sql)로 변경
- 변경 요약: Snapshot 생성 시점을 sh에서 각 서비스 기동 스크립트(*schema.sql)로 변경
- 핵심 로직: DB 스키마/테스트 데이터
- 구조 변화: 쿠폰/포인트 서비스의 계약 또는 테스트 동시 확장
- 주요 파일: `bin_common/02_prepare_k8s_order_saga_Local_Consumer.sh`, `bin_k8s/sql/create_test_snapshots.sql`
- 코드 발췌: `bin_common/02_prepare_k8s_order_saga_Local_Consumer.sh`
```diff
+# 6) MSA 재기동 및 포트포워드
+echo "==> [6/8] MSA 재기동 및 포트포워드"
+kubectl -n msa rollout status deployment/coupon-service
+kubectl -n msa rollout status deployment/point-service
+# 7) 테스트 스냅샷 준비
+echo "==> [7/8] 테스트 스냅샷 준비"
+"${ROOT_DIR}/bin_common/04_create_test_snapshot_procs.sh"
```
- 코드 발췌: `bin_k8s/sql/create_test_snapshots.sql`
```
CREATE DATABASE IF NOT EXISTS coupon_db;
USE coupon_db;

DROP PROCEDURE IF EXISTS sp_reset_coupon_test_data;
DELIMITER $$
CREATE PROCEDURE sp_reset_coupon_test_data()
BEGIN
  TRUNCATE TABLE coupon;
```

### c4401c7 Istio 설치. 강제 타임아웃 테스트용 로직 추가, *** 보상 트랜잭션 타이밍 오류 존재함 ***
- 변경 요약: Istio 설치. 강제 타임아웃 테스트용 로직 추가, *** 보상 트랜잭션 타이밍 오류 존재함 ***
- 핵심 로직: 회로 차단/타임아웃 구성
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `bin_common/00_prepare_mysql_kafka.sh`, `bin_common/02_prepare_k8s_order_saga_Local_Consumer.sh`
- 코드 발췌: `bin_common/00_prepare_mysql_kafka.sh`
```diff
+#!/usr/bin/env bash
+set -euo pipefail
+
+COMMON_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
+# shellcheck disable=SC1091
+source "${COMMON_DIR}/lib.sh"
+
+# 1) MySQL/Kafka 포트포워드 정리
```
- 코드 발췌: `bin_common/02_prepare_k8s_order_saga_Local_Consumer.sh`
```diff
+#!/usr/bin/env bash
+set -euo pipefail
+
+COMMON_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
+# shellcheck disable=SC1091
+source "${COMMON_DIR}/lib.sh"
+
+ISTIO_ENABLED="${ISTIO_ENABLED:-false}"
```

### a16fa0c Timing Issue를 잡기 위한 로직 추가
- 변경 요약: Timing Issue를 잡기 위한 로직 추가
- 핵심 로직: 핵심 로직 추가/구조 변경
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `bin_k8s/sql/create_test_snapshots.sql`, `coupon-service/src/main/java/com/example/couponservice/adapter/out/persistence/CouponReservationPersistenceAdapter.java`
- 코드 발췌: `bin_k8s/sql/create_test_snapshots.sql`
```diff
+CREATE TABLE IF NOT EXISTS coupon_reservation_snapshot LIKE coupon_reservation;
+TRUNCATE TABLE coupon_reservation_snapshot;
+INSERT INTO coupon_reservation_snapshot
+SELECT *
+FROM coupon_reservation;
+
+  TRUNCATE TABLE coupon_reservation;
+  INSERT INTO coupon_reservation SELECT * FROM coupon_reservation_snapshot;
```
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/adapter/out/persistence/CouponReservationPersistenceAdapter.java`
```diff
+package com.example.couponservice.adapter.out.persistence;
+
+import com.example.couponservice.adapter.out.persistence.jpa.CouponReservationJpaEntity;
+import com.example.couponservice.adapter.out.persistence.jpa.CouponReservationJpaRepository;
+import com.example.couponservice.application.port.out.LoadCouponReservationPort;
+import com.example.couponservice.application.port.out.SaveCouponReservationPort;
+import com.example.couponservice.domain.model.CouponReservation;
+import com.example.couponservice.domain.model.status.ReservationStatus;
```

### bfa985f 타이밍 이슈 작업 완료
- 변경 요약: 타이밍 이슈 작업 완료
- 핵심 로직: API 엔드포인트 처리 흐름
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`, `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/webclient/CouponServiceClient.java`
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`
```diff
+import com.example.orderorchestrator.application.service.OrderSagaEventService;
+import com.example.orderorchestrator.application.service.ReserveExternalResourcesService;
+    private final ReserveExternalResourcesService reserveExternalResourcesService;
+    private final OrderSagaEventService orderSagaEventService;
+        return reserveExternalResourcesService.reserveExternalResources(
+                        result.orderId(),
+                        request.couponNumber(),
+                        request.pointNumber()
```
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/webclient/CouponServiceClient.java`
```diff
+import com.example.orderorchestrator.adapter.out.webclient.dto.WebApiResponse;
+import com.example.orderorchestrator.application.port.out.ReserveCouponPort;
+public class CouponServiceClient implements ReserveCouponPort {
+    @Override
+    public Mono<Void> reserveCoupon(String couponNumber, String orderId) {
+                .bodyToMono(new ParameterizedTypeReference<WebApiResponse<ReserveCouponResponse>>() {})
+                })
+                .then();
```

### 1864862 timing issue 테스트 케이스 추가
- 변경 요약: timing issue 테스트 케이스 추가
- 핵심 로직: 테스트 케이스 확장
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `bin_k8s/sql/QueryTestData.sql`, `bin_k8s/sql/create_test_snapshots.sql`
- 코드 발췌: `bin_k8s/sql/QueryTestData.sql`
```diff
+SELECT * FROM order_orchestrator_db.OUTBOX_MESSAGE ;
+SELECT * FROM order_orchestrator_db.ORDER_SAGA ;
+SELECT * FROM order_orchestrator_db.ORDER_ITEM ;
+
+SELECT * FROM point_db.point;
+SELECT * FROM point_db.point_reservation;
+SELECT * FROM coupon_db.coupon;
+SELECT * FROM coupon_db.coupon_reservation;
```
- 코드 발췌: `bin_k8s/sql/create_test_snapshots.sql`
```
CREATE DATABASE IF NOT EXISTS coupon_db;
USE coupon_db;

CREATE TABLE IF NOT EXISTS coupon_snapshot LIKE coupon;
TRUNCATE TABLE coupon_snapshot;
INSERT INTO coupon_snapshot
SELECT *
FROM coupon;
```

### eeca8aa Saga 반복 테스트 가능하도록 데이터 초기화(OUTBOX_MESSAGE, ORDER_SAGA, ORDER_ITEM)
- 변경 요약: Saga 반복 테스트 가능하도록 데이터 초기화(OUTBOX_MESSAGE, ORDER_SAGA, ORDER_ITEM)
- 핵심 로직: 테스트 케이스 확장
- 구조 변화: 운영/실행 스크립트 표준화
- 주요 파일: `bin_common/05_reset_test_data.sh`, `bin_k8s/sql/create_test_snapshots.sql`
- 코드 발췌: `bin_common/05_reset_test_data.sh`
```diff
+  mysql -uroot -prootpw -e "CALL order_orchestrator_db.sp_truncate_order_orchestrator_test_data(); CALL coupon_db.sp_reset_coupon_test_data(); CALL point_db.sp_reset_point_test_data();"
```
- 코드 발췌: `bin_k8s/sql/create_test_snapshots.sql`
```diff
+CREATE DATABASE IF NOT EXISTS order_orchestrator_db;
+USE order_orchestrator_db;
+
+DROP PROCEDURE IF EXISTS sp_truncate_order_orchestrator_test_data;
+DELIMITER $$
+CREATE PROCEDURE sp_truncate_order_orchestrator_test_data()
+BEGIN
+  SET FOREIGN_KEY_CHECKS = 0;
```

### c1100d4 Snapshot 생성 시점을 sh에서 각 서비스 기동 스크립트(*schema.sql)로 변경
- 변경 요약: Snapshot 생성 시점을 sh에서 각 서비스 기동 스크립트(*schema.sql)로 변경
- 핵심 로직: DB 스키마/테스트 데이터
- 구조 변화: 쿠폰/포인트 서비스의 계약 또는 테스트 동시 확장
- 주요 파일: `bin_common/02_prepare_k8s_order_saga_Local_Consumer.sh`, `bin_k8s/sql/create_test_snapshots.sql`
- 코드 발췌: `bin_common/02_prepare_k8s_order_saga_Local_Consumer.sh`
```diff
+# 6) MSA 재기동 및 포트포워드
+echo "==> [6/8] MSA 재기동 및 포트포워드"
+kubectl -n msa rollout status deployment/coupon-service
+kubectl -n msa rollout status deployment/point-service
+# 7) 테스트 스냅샷 준비
+echo "==> [7/8] 테스트 스냅샷 준비"
+"${ROOT_DIR}/bin_common/04_create_test_snapshot_procs.sh"
```
- 코드 발췌: `bin_k8s/sql/create_test_snapshots.sql`
```
CREATE DATABASE IF NOT EXISTS coupon_db;
USE coupon_db;

DROP PROCEDURE IF EXISTS sp_reset_coupon_test_data;
DELIMITER $$
CREATE PROCEDURE sp_reset_coupon_test_data()
BEGIN
  TRUNCATE TABLE coupon;
```

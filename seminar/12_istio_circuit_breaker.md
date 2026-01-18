# 12. Istio 회로 차단과 강제 지연 테스트

## 목표
- Istio 기반 회로 차단과 timeout 테스트를 재현한다.

## 스토리라인
- 장애가 연속되면 회로 차단이 자동으로 열리고, 회복되는지를 검증.

## 관련 커밋
- `327490d`, `c4401c7`, `8e49e95`, `6161467`, `4b031ed`, `987a667`

## 커밋 변경 요약
| 커밋 | 주요 변경 요약 | 체크아웃 |
| --- | --- | --- |
| `327490d` | istio 설치 및 실행 | `git checkout 327490d` |
| `c4401c7` | Istio 설치. 강제 타임아웃 테스트용 로직 추가, *** 보상 트랜잭션 타이밍 오류 존재함 *** | `git checkout c4401c7` |
| `8e49e95` | SH 정리, Codex에 istio, circuit-breaker 관련 프롬프트 정리 | `git checkout 8e49e95` |
| `6161467` | Istio 설치 경로와 yaml 설정 파일 분리 및 istio 설치 경로 git 제외 | `git checkout 6161467` |
| `4b031ed` | *** Timeout Test용 강제 지연 로직을 SRP, OCP 등을 적용해 Decorator 패턴으로 분리. test, dev Profile에서만 사용하도록 변경 | `git checkout 4b031ed` |
| `987a667` | 04_test_circuit_breaker.sh 정리 - 미사용 변수(쿠폰,포인트), max_time 제거, 불필요한 분기 정리, for 반복 실패 횟수 간략화 | `git checkout 987a667` |

## 핵심 개념
- DestinationRule/VirtualService 설정
- 강제 지연(Decorator 패턴)로 타임아웃 유도

## 기술/기능/프로세스
- 기술: Istio/Envoy, DestinationRule/VirtualService
- 기능: timeout, circuit breaker, 강제 지연 테스트
- MSA: 장애 격리와 회복 검증
- EDA: 이벤트 처리 중 장애 전파 방지
## 데모/실습
- `bin_istio_test/04_test_circuit_breaker.sh`
- 지연 조건: `coupon-service/.../ReserveCouponDelayDecorator.java`, `point-service/.../ReservePointDelayDecorator.java`

## 코드 발췌 및 설명
- `bin_k8s/istio/config/circuit-breaker.yaml`: 회로 차단(OutlierDetection)과 강제 지연
```yaml
spec:
  host: coupon-service.msa.svc.cluster.local
  trafficPolicy:
    outlierDetection:
      consecutive5xxErrors: 3
      interval: 5s
      baseEjectionTime: 10s
      maxEjectionPercent: 100
```
- `coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponDelayDecorator.java`: 지연 조건이 맞으면 Thread.sleep으로 타임아웃 유도
```java
    @Override
    public void reserve(String couponNumber, String orderId) {
        maybeDelay(couponNumber);
        delegate.reserve(couponNumber, orderId);
    }
```
- 왜 필요한가: 회로 차단과 강제 지연의 실제 설정/코드를 보여줘, 장애 테스트가 재현되는 이유를 설명할 수 있다.

## 커밋 상세
### 327490d istio 설치 및 실행
- 변경 요약: istio 설치 및 실행
- 핵심 로직: 회로 차단/타임아웃 구성
- 구조 변화: 운영/실행 스크립트 표준화
- 주요 파일: `bin_istio_test/00_prepare_mysql_kafka.sh`, `bin_istio_test/01_prepare_local_order_saga_test.sh`
- 코드 발췌: `bin_istio_test/00_prepare_mysql_kafka.sh`
```diff
+#!/usr/bin/env bash
+set -euo pipefail
+
+ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
+
+kill_port() {
+  local port="$1"
+  if command -v lsof >/dev/null 2>&1; then
```
- 코드 발췌: `bin_istio_test/01_prepare_local_order_saga_test.sh`
```diff
+#!/usr/bin/env bash
+set -euo pipefail
+
+ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
+
+kill_port() {
+  local port="$1"
+  if command -v lsof >/dev/null 2>&1; then
```

### c4401c7 Istio 설치. 강제 타임아웃 테스트용 로직 추가, *** 보상 트랜잭션 타이밍 오류 존재함 ***
- 변경 요약: Istio 설치. 강제 타임아웃 테스트용 로직 추가, *** 보상 트랜잭션 타이밍 오류 존재함 ***
- 핵심 로직: 회로 차단/타임아웃 구성
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `bin_istio_test/00_prepare_mysql_kafka.sh`, `bin_istio_test/02_prepare_k8s_order_saga_Local_Consumer.sh`
- 코드 발췌: `bin_istio_test/00_prepare_mysql_kafka.sh`
```diff
+exec "${ROOT_DIR}/bin_common/00_prepare_mysql_kafka.sh"
```
- 코드 발췌: `bin_istio_test/02_prepare_k8s_order_saga_Local_Consumer.sh`
```diff
+ISTIO_ENABLED=true exec "${ROOT_DIR}/bin_common/02_prepare_k8s_order_saga_Local_Consumer.sh"
```

### 8e49e95 SH 정리, Codex에 istio, circuit-breaker 관련 프롬프트 정리
- 변경 요약: SH 정리, Codex에 istio, circuit-breaker 관련 프롬프트 정리
- 핵심 로직: 회로 차단/타임아웃 구성
- 구조 변화: 운영/실행 스크립트 표준화
- 주요 파일: `bin_istio_test/04_test_circuit_breaker.sh`
- 코드 발췌: `bin_istio_test/04_test_circuit_breaker.sh`
```diff
+echo "==> [1/7] 테스트 데이터 초기화"
+"${ROOT_DIR}/bin_common/05_reset_test_data.sh"
+
+echo "==> [2/7] Istio circuit-breaker 적용"
+echo "==> [3/7] order-orchestrator 포트포워드 확인 (8099)"
+echo "==> [4/7] 정상 호출 1회"
+echo "==> [5/7] timeout 3회 연속 (circuit open 유도)"
+echo "==> [6/7] 2초 대기 (circuit open 유지 예상)"
```

### 6161467 Istio 설치 경로와 yaml 설정 파일 분리 및 istio 설치 경로 git 제외
- 변경 요약: Istio 설치 경로와 yaml 설정 파일 분리 및 istio 설치 경로 git 제외
- 핵심 로직: 회로 차단/타임아웃 구성
- 구조 변화: 운영/실행 스크립트 표준화
- 주요 파일: `bin_istio_test/04_test_circuit_breaker.sh`, `bin_k8s/istio/config/circuit-breaker.yaml`
- 코드 발췌: `bin_istio_test/04_test_circuit_breaker.sh`
```diff
+kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/istio/config/circuit-breaker.yaml"
```
- 코드 발췌: `bin_k8s/istio/config/circuit-breaker.yaml`
```diff
+apiVersion: networking.istio.io/v1beta1
+kind: DestinationRule
+metadata:
+  name: order-orchestrator-dr
+  namespace: msa
+spec:
+  host: order-orchestrator.msa.svc.cluster.local
+  trafficPolicy:
```

### 4b031ed *** Timeout Test용 강제 지연 로직을 SRP, OCP 등을 적용해 Decorator 패턴으로 분리. test, dev Profile에서만 사용하도록 변경
- 변경 요약: *** Timeout Test용 강제 지연 로직을 SRP, OCP 등을 적용해 Decorator 패턴으로 분리. test, dev Profile에서만 사용하도록 변경
- 핵심 로직: 회로 차단/타임아웃 구성
- 구조 변화: 쿠폰/포인트 서비스의 계약 또는 테스트 동시 확장
- 주요 파일: `coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponDelayDecorator.java`, `coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponService.java`
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponDelayDecorator.java`
```diff
+package com.example.couponservice.application.service;
+
+import com.example.couponservice.application.port.in.ReserveCouponUseCase;
+import lombok.RequiredArgsConstructor;
+import org.springframework.beans.factory.annotation.Value;
+import org.springframework.context.annotation.Primary;
+import org.springframework.context.annotation.Profile;
+import org.springframework.stereotype.Service;
```
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponService.java`
```
package com.example.couponservice.application.service;

import com.example.couponservice.application.port.in.CompensateCouponUseCase;
import com.example.couponservice.application.port.in.ConfirmCouponUseCase;
import com.example.couponservice.application.port.in.ReserveCouponUseCase;
import com.example.couponservice.application.port.out.LoadCouponPort;
import com.example.couponservice.application.port.out.SaveCouponPort;
import com.example.couponservice.domain.model.Coupon;
```

### 987a667 04_test_circuit_breaker.sh 정리 - 미사용 변수(쿠폰,포인트), max_time 제거, 불필요한 분기 정리, for 반복 실패 횟수 간략화
- 변경 요약: 04_test_circuit_breaker.sh 정리 - 미사용 변수(쿠폰,포인트), max_time 제거, 불필요한 분기 정리, for 반복 실패 횟수 간략화
- 핵심 로직: 회로 차단/타임아웃 구성
- 구조 변화: 운영/실행 스크립트 표준화
- 주요 파일: `bin_istio_test/04_test_circuit_breaker.sh`
- 코드 발췌: `bin_istio_test/04_test_circuit_breaker.sh`
```diff
+  curl_out="$(curl -s -o /dev/null -w "%{http_code} %{time_total}" -X POST "${ORDER_URL}" \
+      -H "Content-Type: application/json" \
+      --data-binary "${payload}" || true)"
+  echo "${label} -> HTTP ${code} (${total_time}s) (coupon=${coupon_number}, point=${point_number})"
+for i in "${!COUPON_CIRCUIT_ON_LIST[@]}"; do
```

### 327490d istio 설치 및 실행
- 변경 요약: istio 설치 및 실행
- 핵심 로직: 회로 차단/타임아웃 구성
- 구조 변화: 운영/실행 스크립트 표준화
- 주요 파일: `bin_istio_test/00_prepare_mysql_kafka.sh`, `bin_istio_test/01_prepare_local_order_saga_test.sh`
- 코드 발췌: `bin_istio_test/00_prepare_mysql_kafka.sh`
```diff
+#!/usr/bin/env bash
+set -euo pipefail
+
+ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
+
+kill_port() {
+  local port="$1"
+  if command -v lsof >/dev/null 2>&1; then
```
- 코드 발췌: `bin_istio_test/01_prepare_local_order_saga_test.sh`
```diff
+#!/usr/bin/env bash
+set -euo pipefail
+
+ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
+
+kill_port() {
+  local port="$1"
+  if command -v lsof >/dev/null 2>&1; then
```

### c4401c7 Istio 설치. 강제 타임아웃 테스트용 로직 추가, *** 보상 트랜잭션 타이밍 오류 존재함 ***
- 변경 요약: Istio 설치. 강제 타임아웃 테스트용 로직 추가, *** 보상 트랜잭션 타이밍 오류 존재함 ***
- 핵심 로직: 회로 차단/타임아웃 구성
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `bin_istio_test/00_prepare_mysql_kafka.sh`, `bin_istio_test/02_prepare_k8s_order_saga_Local_Consumer.sh`
- 코드 발췌: `bin_istio_test/00_prepare_mysql_kafka.sh`
```diff
+exec "${ROOT_DIR}/bin_common/00_prepare_mysql_kafka.sh"
```
- 코드 발췌: `bin_istio_test/02_prepare_k8s_order_saga_Local_Consumer.sh`
```diff
+ISTIO_ENABLED=true exec "${ROOT_DIR}/bin_common/02_prepare_k8s_order_saga_Local_Consumer.sh"
```

### 8e49e95 SH 정리, Codex에 istio, circuit-breaker 관련 프롬프트 정리
- 변경 요약: SH 정리, Codex에 istio, circuit-breaker 관련 프롬프트 정리
- 핵심 로직: 회로 차단/타임아웃 구성
- 구조 변화: 운영/실행 스크립트 표준화
- 주요 파일: `bin_istio_test/04_test_circuit_breaker.sh`
- 코드 발췌: `bin_istio_test/04_test_circuit_breaker.sh`
```diff
+echo "==> [1/7] 테스트 데이터 초기화"
+"${ROOT_DIR}/bin_common/05_reset_test_data.sh"
+
+echo "==> [2/7] Istio circuit-breaker 적용"
+echo "==> [3/7] order-orchestrator 포트포워드 확인 (8099)"
+echo "==> [4/7] 정상 호출 1회"
+echo "==> [5/7] timeout 3회 연속 (circuit open 유도)"
+echo "==> [6/7] 2초 대기 (circuit open 유지 예상)"
```

### 6161467 Istio 설치 경로와 yaml 설정 파일 분리 및 istio 설치 경로 git 제외
- 변경 요약: Istio 설치 경로와 yaml 설정 파일 분리 및 istio 설치 경로 git 제외
- 핵심 로직: 회로 차단/타임아웃 구성
- 구조 변화: 운영/실행 스크립트 표준화
- 주요 파일: `bin_istio_test/04_test_circuit_breaker.sh`, `bin_k8s/istio/config/circuit-breaker.yaml`
- 코드 발췌: `bin_istio_test/04_test_circuit_breaker.sh`
```diff
+kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/istio/config/circuit-breaker.yaml"
```
- 코드 발췌: `bin_k8s/istio/config/circuit-breaker.yaml`
```diff
+apiVersion: networking.istio.io/v1beta1
+kind: DestinationRule
+metadata:
+  name: order-orchestrator-dr
+  namespace: msa
+spec:
+  host: order-orchestrator.msa.svc.cluster.local
+  trafficPolicy:
```

### 4b031ed *** Timeout Test용 강제 지연 로직을 SRP, OCP 등을 적용해 Decorator 패턴으로 분리. test, dev Profile에서만 사용하도록 변경
- 변경 요약: *** Timeout Test용 강제 지연 로직을 SRP, OCP 등을 적용해 Decorator 패턴으로 분리. test, dev Profile에서만 사용하도록 변경
- 핵심 로직: 회로 차단/타임아웃 구성
- 구조 변화: 쿠폰/포인트 서비스의 계약 또는 테스트 동시 확장
- 주요 파일: `coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponDelayDecorator.java`, `coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponService.java`
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponDelayDecorator.java`
```diff
+package com.example.couponservice.application.service;
+
+import com.example.couponservice.application.port.in.ReserveCouponUseCase;
+import lombok.RequiredArgsConstructor;
+import org.springframework.beans.factory.annotation.Value;
+import org.springframework.context.annotation.Primary;
+import org.springframework.context.annotation.Profile;
+import org.springframework.stereotype.Service;
```
- 코드 발췌: `coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponService.java`
```
package com.example.couponservice.application.service;

import com.example.couponservice.application.port.in.CompensateCouponUseCase;
import com.example.couponservice.application.port.in.ConfirmCouponUseCase;
import com.example.couponservice.application.port.in.ReserveCouponUseCase;
import com.example.couponservice.application.port.out.LoadCouponPort;
import com.example.couponservice.application.port.out.SaveCouponPort;
import com.example.couponservice.domain.model.Coupon;
```

### 987a667 04_test_circuit_breaker.sh 정리 - 미사용 변수(쿠폰,포인트), max_time 제거, 불필요한 분기 정리, for 반복 실패 횟수 간략화
- 변경 요약: 04_test_circuit_breaker.sh 정리 - 미사용 변수(쿠폰,포인트), max_time 제거, 불필요한 분기 정리, for 반복 실패 횟수 간략화
- 핵심 로직: 회로 차단/타임아웃 구성
- 구조 변화: 운영/실행 스크립트 표준화
- 주요 파일: `bin_istio_test/04_test_circuit_breaker.sh`
- 코드 발췌: `bin_istio_test/04_test_circuit_breaker.sh`
```diff
+  curl_out="$(curl -s -o /dev/null -w "%{http_code} %{time_total}" -X POST "${ORDER_URL}" \
+      -H "Content-Type: application/json" \
+      --data-binary "${payload}" || true)"
+  echo "${label} -> HTTP ${code} (${total_time}s) (coupon=${coupon_number}, point=${point_number})"
+for i in "${!COUPON_CIRCUIT_ON_LIST[@]}"; do
```

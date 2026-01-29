# Chapter 9: 서킷 브레이커 동작 테스트 및 안정성 검증

## 1. 개요: 복원력 테스트의 중요성

이전 챕터에서 우리는 Istio를 활용하여 서비스 간의 트래픽에 서킷 브레이커 정책을 적용했습니다. 하지만 이러한 복원력 메커니즘이 실제 장애 상황에서 우리가 의도한 대로 동작하는지, 그리고 시스템이 이러한 장애를 얼마나 잘 견디고 복구되는지를 검증하는 것이 매우 중요합니다. 본 챕터에서는 구축한 서킷 브레이커가 실제 장애 상황(지연, 타임아웃)에서 어떻게 동작하는지 검증하는 테스트 시나리오와 환경을 구축합니다.

### 핵심 학습 목표
*   분산 시스템에서 복원력 테스트의 중요성을 이해합니다.
*   강제 지연 로직을 활용하여 타임아웃 및 서킷 브레이커 동작을 유도하는 방법을 학습합니다.
*   `bin_istio_test` 스크립트를 통해 서킷 브레이커의 열림(Open), 닫힘(Closed), 반쯤 열림(Half-Open) 상태를 확인하고, 시스템의 안정적인 복구 과정을 검증합니다.
*   장애 발생 시 Saga 보상 트랜잭션이 성공적으로 수행되는지 확인하는 테스트 방법을 익힙니다.

## 2. 강제 지연 로직 (Decorator Pattern)

Istio 서킷 브레이커의 동작을 테스트하려면 서비스에 인위적인 지연(Delay)을 주어 타임아웃을 유발해야 합니다. 이를 위해 `coupon-service`와 `point-service`에는 특정 조건에서 응답에 지연을 추가하는 로직이 구현되어 있습니다. 이 로직은 Hexagonal Architecture의 `UseCase` 계층에 **Decorator 패턴**을 사용하여 적용되었으며, `dev` 또는 `test` 프로파일에서만 활성화됩니다.

**`circuit-test.coupon.delay-prefix`**와 일치하는 쿠폰 번호를 사용하면, 설정된 `circuit-test.coupon.delay-ms`만큼의 지연이 발생합니다. 이는 Istio `VirtualService`에 설정된 `timeout` (`2s`)보다 긴 지연 시간을 주어 타임아웃을 유도하고, 이로 인해 `consecutive5xxErrors`가 누적되어 서킷 브레이커가 `Open` 상태로 전환되도록 합니다.

## 3. 복원력 테스트 관련 Git 이력

강제 지연 로직 구현, Istio 서킷 브레이커 테스트 스크립트 추가 및 개선과 관련된 주요 Git 커밋입니다.

| 커밋 ID | 날짜 | 주요 변경 요약 |
|---|---|---|
| `c4401c7` | 2026-01-14 | 강제 타임아웃 테스트용 로직 추가 |
| `8e49e95` | 2026-01-14 | `bin_istio_test` 스크립트 정리 및 관련 프롬프트 정리 |
| `4b031ed` | 2026-01-15 | 타임아웃 테스트용 강제 지연 로직을 Decorator 패턴으로 분리 |
| `987a667` | 2026-01-15 | `04_test_circuit_breaker.sh` 스크립트 정리 |

**(실습 가이드: Git 커밋 확인)**
1.  `git checkout 4b031ed` 명령어로 해당 커밋 시점으로 이동하여 `ReserveCouponDelayDecorator.java`와 `ReservePointDelayDecorator.java` 파일을 확인해 보세요. Decorator 패턴을 통해 어떻게 지연 로직이 주입되었는지 볼 수 있습니다.
2.  `git diff 4b031ed~1 987a667` 명령어로 `04_test_circuit_breaker.sh` 스크립트가 리팩토링된 변경사항을 확인할 수 있습니다.

## 4. 핵심 코드 스니펫: 복원력 테스트 구현

### 4.1. `ReserveCouponDelayDecorator` (강제 지연 로직)

`ReserveCouponService`의 `reserve` 메서드에 지연 기능을 추가하는 Decorator 패턴 구현체입니다. `application.yaml`에 설정된 조건(`delay-enabled`, `delay-prefix`, `delay-ms`)에 따라 특정 쿠폰 번호에 대해 강제로 지연을 발생시킵니다.

**`coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponDelayDecorator.java`**
```java
// ... imports ...
@Service
@Primary // 동일한 타입의 빈이 여러 개일 경우 이 빈을 우선적으로 주입
@Profile({"dev", "test"}) // 'dev' 또는 'test' 프로파일에서만 활성화
@RequiredArgsConstructor
public class ReserveCouponDelayDecorator implements ReserveCouponUseCase {

    private final ReserveCouponService delegate; // 실제 ReserveCouponUseCase 구현체

    @Value("${circuit-test.coupon.delay-enabled:false}")
    private boolean delayEnabled; // 지연 활성화 여부
    @Value("${circuit-test.coupon.delay-prefix:}")
    private String delayPrefix; // 지연을 발생시킬 쿠폰 번호 접두사
    @Value("${circuit-test.coupon.delay-ms:0}")
    private long delayMs; // 지연 시간 (밀리초)

    @Override
    public void reserve(String couponNumber, String orderId) {
        maybeDelay(couponNumber); // 지연 조건에 맞으면 강제 지연 발생
        delegate.reserve(couponNumber, orderId); // 실제 서비스 로직 호출
    }

    private void maybeDelay(String couponNumber) {
        if (!delayEnabled) return;
        if (delayMs <= 0 || !StringUtils.hasText(delayPrefix)) return;
        if (!couponNumber.startsWith(delayPrefix)) return;

        try {
            System.out.println("### Force Delaying Coupon Service for " + delayMs + "ms for coupon: " + couponNumber);
            Thread.sleep(delayMs); // 강제 지연
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Delay interrupted", ex);
        }
    }
}
```
**`coupon-service/src/main/resources/coupon_application.yaml`** (일부 발췌)
```yaml
circuit-test:
  coupon:
    delay-enabled: true # 지연 기능 활성화
    delay-prefix: CPN-INT-FORCE-DELAY # "CPN-INT-FORCE-DELAY"로 시작하는 쿠폰 번호에 대해 지연 적용
    delay-ms: 8000 # 8초 지연 (Istio timeout 2s보다 김)
```
**설명:** `ReserveCouponDelayDecorator`는 `ReserveCouponUseCase`를 구현하며, 실제 `ReserveCouponService`를 감싸는(decorate) 역할을 합니다. `@Primary`와 `@Profile({"dev", "test"})`를 통해 개발/테스트 환경에서만 이 Decorator가 주입되어 지연 로직이 활성화됩니다. `delay-prefix`와 `delay-ms` 설정을 통해 특정 쿠폰 번호에 대해 의도적인 지연을 발생시켜 Istio의 타임아웃 및 서킷 브레이커를 테스트할 수 있습니다. `point-service`에도 유사한 `ReservePointDelayDecorator`가 구현되어 있습니다.

### 4.2. `04_test_circuit_breaker.sh` (서킷 브레이커 테스트 스크립트)

이 쉘 스크립트는 `order-orchestrator`에 요청을 보내 `coupon-service`와 `point-service`에 인위적인 지연을 유발하고, Istio 서킷 브레이커가 `Open` 되었다가 `Closed` 상태로 복구되는 과정을 테스트합니다.

**`bin_istio_test/04_test_circuit_breaker.sh`** (일부 발췌)
```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ORDER_URL="http://localhost:8099/api/v1/orders" # order-orchestrator 호출 URL

# 서킷 브레이커 테스트용 쿠폰/포인트 번호 (지연 유발용)
COUPON_FORCE_DELAY_LIST=("CPN-INT-FORCE-DELAY1" "CPN-INT-FORCE-DELAY2" "CPN-INT-FORCE-DELAY3")
POINT_FORCE_DELAY_LIST=("PNT-INT-FORCE-DELAY1" "PNT-INT-FORCE-DELAY2" "PNT-INT-FORCE-DELAY3")

# 주문 생성 요청 함수
post_order() {
  local label="$1"
  local coupon_number="$2"
  local point_number="$3"
  # ... (curl을 이용한 HTTP POST 요청 및 응답 코드/시간 추출 로직 생략) ...
  echo "${label} -> HTTP ${code} (${total_time}s) (coupon=${coupon_number}, point=${point_number})"
}

echo "==> [1/7] 테스트 데이터 초기화"
"${ROOT_DIR}/bin_common/05_reset_test_data.sh" # 테스트 데이터 초기화

echo "==> [2/7] Istio circuit-breaker 적용"
kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/istio/config/circuit-breaker.yaml" # 서킷 브레이커 정책 재적용

echo "==> [3/7] order-orchestrator 포트포워드 확인 (8099)"
# ... (order-orchestrator 포트 포워딩 확인 로직 생략) ...

echo "==> [4/7] 정상 호출 1회 (서킷 닫힘 상태 확인)"
post_order "normal-1" "CPN-INT-OK-START" "PNT-INT-OK-START" # 지연 없는 정상 쿠폰/포인트 사용

echo "==> [5/7] timeout 3회 연속 (circuit open 유도)"
for i in "${!COUPON_FORCE_DELAY_LIST[@]}"; do
  # 지연 유발 쿠폰/포인트를 사용하여 타임아웃 유도. 연속 3회 실패 시 서킷 오픈 예상
  post_order "timeout-$((i + 1))" "${COUPON_FORCE_DELAY_LIST[$i]}" "${POINT_FORCE_DELAY_LIST[$i]}"
done

echo "==> [6/7] 2초 대기 (circuit open 유지 예상) 및 호출"
sleep 2 # 서킷 오픈 유지 시간 (baseEjectionTime: 10s) 고려
post_order "after-2s" "CPN-INT-AFTER-OPEN" "PNT-INT-AFTER-OPEN" # 이 호출은 서킷 오픈으로 인해 빠르게 실패할 예상

echo "==> [7/7] 총 15초 경과 후 호출 (circuit 정상 여부 확인)"
sleep 13 # baseEjectionTime(10s) + 3s 대기. 서킷이 Half-Open을 거쳐 Closed로 복구될 시간
post_order "after-15s" "CPN-INT-AFTER-RECOVER" "PNT-INT-AFTER-RECOVER" # 이 호출은 성공할 예상
```
**설명:** 이 스크립트는 다음과 같은 시나리오로 서킷 브레이커를 테스트합니다.
1.  **초기화:** 테스트 데이터 초기화 및 Istio 서킷 브레이커 정책 적용.
2.  **정상 호출:** 서킷이 `Closed` 상태임을 확인하기 위한 정상적인 요청.
3.  **타임아웃 유도:** 강제 지연이 설정된 쿠폰/포인트 번호를 사용하여 `order-orchestrator`에 요청을 보냅니다. Istio `VirtualService`의 `timeout: 2s`로 인해 `coupon-service`/`point-service`의 `8s` 지연은 타임아웃으로 처리되고 5xx 에러로 간주됩니다. `DestinationRule`의 `consecutive5xxErrors: 3`에 따라 3회 연속 실패 후 서킷이 `Open` 상태로 전환됩니다.
4.  **서킷 오픈 상태 확인:** 서킷이 `Open`된 후에는 즉시 실패 응답을 받게 됩니다.
5.  **서킷 복구 확인:** `baseEjectionTime: 10s` 이후에는 서킷이 `Half-Open` 상태로 전환되고, 이후 테스트 요청이 성공하면 `Closed` 상태로 완전히 복구됩니다.

### 4.3. `05_test_saga_compensation.sh` (Saga 보상 테스트 스크립트)

이 스크립트는 특정 서비스(쿠폰 또는 포인트)에 고의적인 실패를 유발하여 Saga 보상 트랜잭션이 정상적으로 수행되는지 검증합니다.

**`bin_istio_test/05_test_saga_compensation.sh`** (일부 발췌)
```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ORDER_URL="http://localhost:8099/api/v1/orders"

# 강제 실패 유발용 쿠폰/포인트 번호
COUPON_FAIL="CPN-INT-FORCE-DELAY1" # 실패 유도 (타임아웃)
POINT_OK="PNT-INT-OK-START"

COUPON_OK="CPN-INT-OK-START"
POINT_FAIL="PNT-INT-FORCE-DELAY2"

# ... (post_order 함수 생략) ...

# 쿠폰/포인트 상태를 DB에서 직접 조회하는 함수
fetch_coupon_status() {
  local coupon_number="$1"
  kubectl -n msa exec -i deploy/mysql -- \
    mysql -uroot -prootpw -N -e \
      "select status from coupon_db.coupon where coupon_number='${coupon_number}';" | tr -d '\r'
}

fetch_point_status() {
  local point_number="$1"
  kubectl -n msa exec -i deploy/mysql -- \
    mysql -uroot -prootpw -N -e \
      "select status from point_db.point where point_number='${point_number}';" | tr -d '\r'
}

# 쿠폰/포인트 상태가 AVAILABLE로 복구될 때까지 기다리는 함수
wait_for_available() {
  local label="$1"
  local coupon_number="$2"
  local point_number="$3"
  local retry=30 # 30초 동안 대기
  # ... (상태 확인 및 대기 로직 생략) ...
  echo "  ${label} status -> coupon=${coupon_status:-N/A}, point=${point_status:-N/A}"
}

echo "==> [1/5] 테스트 데이터 초기화"
"${ROOT_DIR}/bin_common/05_reset_test_data.sh"

echo "==> [2/5] Istio circuit-breaker 적용"
kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/istio/config/circuit-breaker.yaml"

echo "==> [3/5] order-orchestrator 포트포워드 확인 (8099)"
# ... (order-orchestrator 포트 포워딩 확인 로직 생략) ...

echo "==> [4/5] 쿠폰 실패 -> 보상으로 쿠폰/포인트 모두 AVAILABLE 확인"
post_order "coupon-fail" "${COUPON_FAIL}" "${POINT_OK}" # 쿠폰만 실패 유도
wait_for_available "coupon-fail" "${COUPON_FAIL}" "${POINT_OK}" # Saga 보상으로 쿠폰/포인트 모두 AVAILABLE 확인

echo "==> [5/5] 포인트 실패 -> 보상으로 쿠폰/포인트 모두 AVAILABLE 확인"
post_order "point-fail" "${COUPON_OK}" "${POINT_FAIL}" # 포인트만 실패 유도
wait_for_available "point-fail" "${COUPON_OK}" "${POINT_FAIL}" # Saga 보상으로 쿠폰/포인트 모두 AVAILABLE 확인
```
**설명:** 이 스크립트는 `post_order`를 통해 특정 쿠폰/포인트 번호를 사용하여 `order-orchestrator`에 요청을 보냅니다. 이때, 강제 지연 로직으로 인해 `coupon-service` 또는 `point-service`가 타임아웃되어 `order-orchestrator`는 Saga를 `Compensating` 상태로 전환하고 이벤트를 발행합니다. `wait_for_available` 함수는 `kubectl exec`를 통해 MySQL 파드에 직접 접속하여 `coupon_db.coupon` 및 `point_db.point` 테이블의 상태를 주기적으로 조회하며, Saga 보상이 완료되어 쿠폰/포인트의 상태가 다시 `AVAILABLE`로 복구되는지를 확인합니다.

## 5. 실습 체크포인트

### 5.1. 서킷 브레이커 동작 테스트
1.  **필수 서비스 실행:**
    *   Chapter 8에서 Istio가 설치되고 `msa` 네임스페이스에 사이드카 주입이 활성화되었으며, 모든 MSA 애플리케이션 파드가 `2/2` 상태로 실행 중인지 확인합니다.
    *   모든 MSA 애플리케이션(`coupon-service`, `point-service`, `order-orchestrator`, `order-saga-consumer`)이 K8s에 배포되어 실행 중이어야 합니다. `bin_k8s/03_deploy_all.sh`와 `bin_k8s/05_msa_portforward.sh`를 통해 실행할 수 있습니다.
2.  **`bin_istio_test/04_test_circuit_breaker.sh` 실행:**
    *   프로젝트 루트에서 `./bin_istio_test/04_test_circuit_breaker.sh` 스크립트를 실행합니다.
    *   **예상 결과:**
        *   "normal-1" 요청은 `HTTP 200`으로 성공하고 응답 시간이 짧을 것입니다.
        *   "timeout-1", "timeout-2", "timeout-3" 요청은 `HTTP 5xx` (Gateway Timeout 등) 에러를 반환하며 응답 시간이 2초에 가까울 것입니다. (Istio timeout 설정)
        *   "after-2s" 요청은 `HTTP 5xx` 에러를 반환하며 응답 시간이 매우 짧을 것입니다. (서킷 `Open` 상태로 즉시 실패)
        *   "after-15s" 요청은 `HTTP 200`으로 성공하고 응답 시간이 짧을 것입니다. (서킷 `Closed` 상태로 복구)
    *   각 호출의 응답 시간과 HTTP 상태 코드를 통해 서킷 브레이커가 예상대로 `Open`되고 `Closed`로 복구되는 것을 확인할 수 있습니다.

### 5.2. Saga 보상 트랜잭션 테스트
1.  **필수 서비스 실행:**
    *   Chapter 8에서 Istio가 설치되고 `msa` 네임스페이스에 사이드카 주입이 활성화되었으며, 모든 MSA 애플리케이션 파드가 `2/2` 상태로 실행 중인지 확인합니다.
    *   모든 MSA 애플리케이션(`coupon-service`, `point-service`, `order-orchestrator`, `order-saga-consumer`)이 K8s에 배포되어 실행 중이어야 합니다.
2.  **`bin_istio_test/05_test_saga_compensation.sh` 실행:**
    *   프로젝트 루트에서 `./bin_istio_test/05_test_saga_compensation.sh` 스크립트를 실행합니다.
    *   **예상 결과:**
        *   `coupon-fail` 테스트에서 쿠폰 예약이 실패하고, Saga 보상 트랜잭션이 발생하여 `wait_for_available` 함수가 쿠폰과 포인트 모두 `AVAILABLE` 상태로 복구됨을 확인하고 성공적으로 종료될 것입니다.
        *   `point-fail` 테스트도 동일하게 포인트 예약이 실패하고, Saga 보상 트랜잭션이 발생하여 쿠폰과 포인트 모두 `AVAILABLE` 상태로 복구됨을 확인하고 성공적으로 종료될 것입니다.
    *   스크립트의 로그를 통해 각 서비스의 상태 변화와 Saga 보상이 정상적으로 동작하는 것을 확인할 수 있습니다.

---
복원력 테스트를 통해 우리는 Istio 서킷 브레이커와 Saga 보상 트랜잭션이 분산 시스템의 안정성을 어떻게 확보하는지 실제 동작을 통해 확인했습니다. 이는 복잡한 MSA/EDA 환경에서 시스템이 장애에 강인하게 동작하도록 만드는 핵심적인 요소입니다.

이것으로 `MSA & EDA 기반 주문 시스템 개발 여정`의 주요 챕터는 마무리됩니다. 이 여정을 통해 분산 시스템 설계와 구현에 대한 깊이 있는 이해를 얻으셨기를 바랍니다. 다음은 부록 섹션입니다.

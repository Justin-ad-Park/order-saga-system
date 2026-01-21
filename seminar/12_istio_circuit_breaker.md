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

## 커밋 상세
### 327490d istio 설치 및 실행
- 주요 변경: istio 설치 및 실행

## 테스트 시나리오
`bin_istio_test/04_test_circuit_breaker.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ORDER_URL="http://localhost:8099/api/v1/orders"
COUPON_CIRCUIT_OFF="CPN-INT-OK-START"
POINT_CIRCUIT_OFF="PNT-INT-OK-START"

COUPON_CIRCUIT_OFF2="CPN-INT-AFTER-OPEN"
POINT_CIRCUIT_OFF2="PNT-INT-AFTER-OPEN"

COUPON_CIRCUIT_OFF3="CPN-INT-AFTER-RECOVER"
POINT_CIRCUIT_OFF3="PNT-INT-AFTER-RECOVER"

COUPON_FORCE_DELAY_LIST=("CPN-INT-FORCE-DELAY1" "CPN-INT-FORCE-DELAY2" "CPN-INT-FORCE-DELAY3")
POINT_FORCE_DELAY_LIST=("PNT-INT-FORCE-DELAY1" "PNT-INT-FORCE-DELAY2" "PNT-INT-FORCE-DELAY3")


wait_for_port() {
  local port="$1"
  local retry=20
  while ! lsof -i "tcp:${port}" >/dev/null 2>&1; do
    retry=$((retry - 1))
    if [[ "${retry}" -le 0 ]]; then
      return 1
    fi
    sleep 0.5
  done
}

post_order() {
  local label="$1"
  local coupon_number="$2"
  local point_number="$3"

  local payload
  payload="$(cat <<EOF
{"couponNumber":"${coupon_number}","pointNumber":"${point_number}","paymentNumber":"PAY-${label}","paymentAmount":15000,"orderItems":[{"itemNumber":"ITEM-001","quantity":2}]}
EOF
)"
  local code
  local total_time
  local curl_out
  curl_out="$(curl -s -o /dev/null -w "%{http_code} %{time_total}" -X POST "${ORDER_URL}" \
      -H "Content-Type: application/json" \
      --data-binary "${payload}" || true)"
  code="$(echo "${curl_out}" | awk '{print $1}')"
  total_time="$(echo "${curl_out}" | awk '{print $2}')"

  echo "${label} -> HTTP ${code} (${total_time}s) (coupon=${coupon_number}, point=${point_number})"
}

echo "==> [1/7] 테스트 데이터 초기화"
"${ROOT_DIR}/bin_common/05_reset_test_data.sh"

echo "==> [2/7] Istio circuit-breaker 적용"
kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/istio/config/circuit-breaker.yaml"

echo "==> [3/7] order-orchestrator 포트포워드 확인 (8099)"
if ! lsof -i tcp:8099 >/dev/null 2>&1; then
  kubectl -n msa port-forward svc/order-orchestrator 8099:8099 > "${ROOT_DIR}/order-port-forward.log" 2>&1 &
  wait_for_port 8099
fi

echo "==> [4/7] 정상 호출 1회"
post_order "normal-1" "${COUPON_CIRCUIT_OFF}" "${POINT_CIRCUIT_OFF}"

echo "==> [5/7] timeout 3회 연속 (circuit open 유도)"
for i in "${!COUPON_FORCE_DELAY_LIST[@]}"; do
  post_order "timeout-$((i + 1))" "${COUPON_FORCE_DELAY_LIST[$i]}" "${POINT_FORCE_DELAY_LIST[$i]}"
done

echo "==> [6/7] 2초 대기 (circuit open 유지 예상)"
sleep 2
post_order "after-2s" "${COUPON_CIRCUIT_OFF2}" "${POINT_CIRCUIT_OFF2}"

echo "==> [7/7] 총 15초 경과 후 호출 (circuit 정상 여부 확인)"
sleep 13
post_order "after-15s" "${COUPON_CIRCUIT_OFF3}" "${POINT_CIRCUIT_OFF3}"

```

- 설명: 오케스트레이터가 쿠폰/포인트 예약을 병렬 호출해 분산 트랜잭션의 시작점을 만든다.

### 8e49e95 SH 정리, Codex에 istio, circuit-breaker 관련 프롬프트 정리
- 주요 변경: SH 정리, Codex에 istio, circuit-breaker 관련 프롬프트 정리
- 핵심 코드: `bin_istio_test/04_test_circuit_breaker.sh`
```bash
//--- 생략 ...
  fi
}

echo "==> [1/7] 테스트 데이터 초기화"
"${ROOT_DIR}/bin_common/05_reset_test_data.sh"

echo "==> [2/7] Istio circuit-breaker 적용"
kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/istio/circuit-breaker.yaml"

//--- 생략 ...
```
- 설명: 실행/테스트 스크립트를 통해 분산 시나리오를 재현한다.


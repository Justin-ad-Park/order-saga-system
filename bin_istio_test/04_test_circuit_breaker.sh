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

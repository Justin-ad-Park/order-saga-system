#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ORDER_URL="http://localhost:8099/api/v1/orders"

COUPON_FAIL="CPN-INT-FORCE-DELAY1"
POINT_OK="PNT-INT-OK-START"

COUPON_OK="CPN-INT-OK-START"
POINT_FAIL="PNT-INT-FORCE-DELAY2"

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

wait_for_available() {
  local label="$1"
  local coupon_number="$2"
  local point_number="$3"
  local retry=30

  while [[ "${retry}" -gt 0 ]]; do
    local coupon_status
    local point_status
    coupon_status="$(fetch_coupon_status "${coupon_number}")"
    point_status="$(fetch_point_status "${point_number}")"

    echo "  ${label} status -> coupon=${coupon_status:-N/A}, point=${point_status:-N/A}"
    if [[ "${coupon_status}" == "AVAILABLE" && "${point_status}" == "AVAILABLE" ]]; then
      return 0
    fi
    retry=$((retry - 1))
    sleep 1
  done

  echo "  ${label} status check timeout (coupon=${coupon_number}, point=${point_number})" >&2
  return 1
}

echo "==> [1/5] 테스트 데이터 초기화"
"${ROOT_DIR}/bin_common/05_reset_test_data.sh"

echo "==> [2/5] Istio circuit-breaker 적용"
kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/istio/config/circuit-breaker.yaml"

echo "==> [3/5] order-orchestrator 포트포워드 확인 (8099)"
if ! lsof -i tcp:8099 >/dev/null 2>&1; then
  kubectl -n msa port-forward svc/order-orchestrator 8099:8099 > "${ROOT_DIR}/order-port-forward.log" 2>&1 &
  wait_for_port 8099
fi

echo "==> [4/5] 쿠폰 실패 -> 보상으로 쿠폰/포인트 모두 AVAILABLE 확인"
post_order "coupon-fail" "${COUPON_FAIL}" "${POINT_OK}"
wait_for_available "coupon-fail" "${COUPON_FAIL}" "${POINT_OK}"

echo "==> [5/5] 포인트 실패 -> 보상으로 쿠폰/포인트 모두 AVAILABLE 확인"
post_order "point-fail" "${COUPON_OK}" "${POINT_FAIL}"
wait_for_available "point-fail" "${COUPON_OK}" "${POINT_FAIL}"

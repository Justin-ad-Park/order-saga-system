#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ORDER_URL="http://localhost:8099/api/v1/orders"
COUPON_BOTH="CPN-INT-BOTH-001"
COUPON_ONLY="CPN-INT-ONLY-001"
POINT_BOTH="PNT-INT-BOTH-001"
POINT_ONLY="PNT-INT-ONLY-001"

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
  local code
  code="$(curl -s -o /dev/null -w "%{http_code}" -X POST "${ORDER_URL}" \
    -H "Content-Type: application/json" \
    -d "{\"couponNumber\":\"${coupon_number}\",\"pointNumber\":\"${point_number}\",\"paymentNumber\":\"PAY-${label}\",\"paymentAmount\":15000,\"orderItems\":[{\"itemNumber\":\"ITEM-001\",\"quantity\":2}]}" || true)"
  echo "${label} -> HTTP ${code} (coupon=${coupon_number}, point=${point_number})"
}

echo "==> [1/7] Istio circuit-breaker 적용"
kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/istio/circuit-breaker.yaml"

echo "==> [2/7] order-orchestrator 포트포워드 확인 (8099)"
if ! lsof -i tcp:8099 >/dev/null 2>&1; then
  kubectl -n msa port-forward svc/order-orchestrator 8099:8099 > "${ROOT_DIR}/order-port-forward.log" 2>&1 &
  wait_for_port 8099
fi

echo "==> [3/7] 정상 호출 1회"
post_order "normal-1" "${COUPON_BOTH}" "${POINT_BOTH}"

echo "==> [4/7] coupon fault injection 적용 (6s delay)"
kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/istio/coupon-fault-delay.yaml"

echo "==> [5/7] coupon timeout 3회 연속"
for i in 1 2 3; do
  post_order "timeout-${i}" "${COUPON_BOTH}" "${POINT_BOTH}"
done

echo "==> [6/7] fault 제거 후 5초 대기"
kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/istio/circuit-breaker.yaml"
sleep 5
post_order "after-5s" "${COUPON_ONLY}" "${POINT_ONLY}"

echo "==> [7/7] 11초 시점 호출"
sleep 6
post_order "after-11s" "${COUPON_ONLY}" "${POINT_ONLY}"

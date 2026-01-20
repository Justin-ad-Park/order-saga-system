# 06. Resilience tests (delay / circuit / compensation)

## Goal
Validate that the SAGA flow behaves correctly under failures and timeouts.

## Core flow
- Induce delay to open circuit
- Observe failed requests
- Verify compensation makes resources AVAILABLE

## Circuit breaker scenario
`bin_istio_test/04_test_circuit_breaker.sh`
```bash
ORDER_URL="http://localhost:8099/api/v1/orders"
COUPON_CIRCUIT_OFF="CPN-INT-OK-START"
POINT_CIRCUIT_OFF="PNT-INT-OK-START"

COUPON_FORCE_DELAY_LIST=("CPN-INT-FORCE-DELAY1" "CPN-INT-FORCE-DELAY2" "CPN-INT-FORCE-DELAY3")
POINT_FORCE_DELAY_LIST=("PNT-INT-FORCE-DELAY1" "PNT-INT-FORCE-DELAY2" "PNT-INT-FORCE-DELAY3")

post_order "normal-1" "${COUPON_CIRCUIT_OFF}" "${POINT_CIRCUIT_OFF}"

for i in "${!COUPON_FORCE_DELAY_LIST[@]}"; do
  post_order "timeout-$((i + 1))" "${COUPON_FORCE_DELAY_LIST[$i]}" "${POINT_FORCE_DELAY_LIST[$i]}"
done
```

## Compensation verification
`bin_istio_test/05_test_saga_compensation.sh`
```bash
COUPON_FAIL="CPN-INT-FORCE-DELAY1"
POINT_OK="PNT-INT-OK-START"

COUPON_OK="CPN-INT-OK-START"
POINT_FAIL="PNT-INT-FORCE-DELAY2"

post_order "coupon-fail" "${COUPON_FAIL}" "${POINT_OK}"
wait_for_available "coupon-fail" "${COUPON_FAIL}" "${POINT_OK}"

post_order "point-fail" "${COUPON_OK}" "${POINT_FAIL}"
wait_for_available "point-fail" "${COUPON_OK}" "${POINT_FAIL}"
```

## Hands-on checkpoints
- Ensure circuit open/close behavior is visible in response times
- Confirm coupon/point rows return to AVAILABLE after compensation

# 13. int_test -> main

## 시점
- 2026-01-16

## 비교 기준
- 직전 main 상태: `1864862250cc01498644313b40eb668959067809`
- 브랜치 tip: `c1100d4`

## 주요 변경(커밋 메시지 기반)
- Snapshot 생성 시점을 sh에서 각 서비스 기동 스크립트(*schema.sql)로 변경

## MSA + EDA + SAGA 관점 요약
- 오케스트레이터 흐름 추가/수정
- 쿠폰 서비스 변경
- 포인트 서비스 변경
- Istio 테스트/서킷 관련 스크립트
- K8S/Kafka 배포 및 운영 스크립트
- DB 스키마/테스트 데이터 정리

## 연결된 로직 흐름
- 쿠폰 서비스 처리 -> 포인트 서비스 처리 -> 실패/회복 테스트 시나리오

## 핵심 로직 스니펫(머지 시점 기준)
- `coupon-service/src/main/resources/coupon_application.yaml`
```yaml
# src/main/resources/application.yml
spring:
  profiles:
    active: test

---
spring:
  config:
    activate:
      on-profile: test

  datasource:
    url: jdbc:mysql://localhost:3307/coupon_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
    username: coupon_user
    password: coupon_pw

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    defer-datasource-initialization: true

  sql:
    init:
      mode: always
      schema-locations: classpath:coupon_schema.sql
  #    init:
  #      mode: embedded  #always | never | embedded

server:
  port: 8081

circuit-test:
  coupon:
    delay-enabled: true
    delay-prefix: CPN-INT-FORCE-DELAY
    delay-ms: 8000

---
spring:
  config:
    activate:
      on-profile: dev

  datasource:
    url: jdbc:mysql://mysql.msa.svc.cluster.local:3306/coupon_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
    username: coupon_user
    password: ${COUPON_DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    defer-datasource-initialization: true

  sql:
    init:
      mode: always
      schema-locations: classpath:coupon_schema.sql
  #    init:
  #      mode: embedded  #always | never | embedded

server:
  port: 8081

circuit-test:
  coupon:
    delay-enabled: true
    delay-prefix: CPN-INT-FORCE-DELAY
    delay-ms: 8000
```
- `coupon-service/src/main/resources/coupon_schema.sql`
```sql
CREATE TABLE IF NOT EXISTS coupon (
                                      coupon_number VARCHAR(255) PRIMARY KEY,
    status VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expired_at TIMESTAMP NOT NULL
    );

CREATE TABLE IF NOT EXISTS coupon_reservation (
    order_id VARCHAR(255) PRIMARY KEY,
    coupon_number VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
    );

TRUNCATE TABLE coupon;
TRUNCATE TABLE coupon_reservation;

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-INT-BOTH-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-INT-BOTH-RESERVED-001',
           'RESERVED',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-INT-ONLY-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-INT-ONLY-002',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-SVC-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-INT-AVAILABLE-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-INT-RESERVED-001',
           'RESERVED',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-INT-CONFIRM-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-INT-COMPENSATE-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);


INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-INT-FORCE-DELAY1',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-INT-FORCE-DELAY2',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-INT-FORCE-DELAY3',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-INT-OK-START',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-INT-AFTER-OPEN',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'CPN-INT-AFTER-RECOVER',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);


CREATE TABLE IF NOT EXISTS coupon_snapshot LIKE coupon;
TRUNCATE TABLE coupon_snapshot;
INSERT INTO coupon_snapshot
SELECT *
FROM coupon;
```
- `point-service/src/main/resources/point_application.yaml`
```yaml
# src/main/resources/application.yml
spring:
  profiles:
    active: test

---
spring:
  config:
    activate:
      on-profile: test

  datasource:
    url: jdbc:mysql://localhost:3307/point_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
    username: point_user
    password: point_pw

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    defer-datasource-initialization: true

  sql:
    init:
      mode: always
      schema-locations: classpath:point_schema.sql
  #    init:
  #      mode: embedded  #always | never | embedded

server:
  port: 8082

circuit-test:
  point:
    delay-enabled: true
    delay-prefix: PNT-INT-FORCE-DELAY
    delay-ms: 8000

---
spring:
  config:
    activate:
      on-profile: dev

  datasource:
    url: jdbc:mysql://mysql.msa.svc.cluster.local:3306/point_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
    username: point_user
    password: ${POINT_DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    defer-datasource-initialization: true

  sql:
    init:
      mode: always
      schema-locations: classpath:point_schema.sql
  #    init:
  #      mode: embedded  #always | never | embedded

server:
  port: 8082

circuit-test:
  point:
    delay-enabled: true
    delay-prefix: PNT-INT-FORCE-DELAY
    delay-ms: 8000
```
- `point-service/src/main/resources/point_schema.sql`
```sql
CREATE TABLE IF NOT EXISTS point (
                                     point_number VARCHAR(255) PRIMARY KEY,
    status VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expired_at TIMESTAMP NOT NULL
    );

CREATE TABLE IF NOT EXISTS point_reservation (
    order_id VARCHAR(255) PRIMARY KEY,
    point_number VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
    );

TRUNCATE TABLE point;
TRUNCATE TABLE point_reservation;

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-INT-BOTH-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-INT-BOTH-AVAILABLE-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-INT-ONLY-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-INT-ONLY-002',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);



INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-SVC-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-INT-AVAILABLE-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-INT-RESERVED-001',
           'RESERVED',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-INT-CONFIRM-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-INT-COMPENSATE-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-INT-FORCE-DELAY1',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-INT-FORCE-DELAY2',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-INT-FORCE-DELAY3',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-INT-OK-START',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-INT-AFTER-OPEN',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'PNT-INT-AFTER-RECOVER',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);

CREATE TABLE IF NOT EXISTS point_snapshot LIKE point;
TRUNCATE TABLE point_snapshot;
INSERT INTO point_snapshot
SELECT *
FROM point;
```
- `bin_istio_test/04_test_circuit_breaker.sh`
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
- `bin_istio_test/05_test_saga_compensation.sh`
```bash
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
```

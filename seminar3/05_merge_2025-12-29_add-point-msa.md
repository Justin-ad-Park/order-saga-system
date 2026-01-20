# 05. add-point-msa -> main

## 시점
- 2025-12-29

## 비교 기준
- 직전 main 상태: `3eb2580bd872e12812d683d1e56da496bda5a968`
- 브랜치 tip: `f61c6fd`

## 주요 변경(커밋 메시지 기반)
- K8s MSA 배포 추가

## MSA + EDA + SAGA 관점 요약
- 오케스트레이터 흐름 추가/수정
- 쿠폰 서비스 변경
- 포인트 서비스 변경
- K8S/Kafka 배포 및 운영 스크립트
- DB 스키마/테스트 데이터 정리

## 연결된 로직 흐름
- 쿠폰 서비스 처리 -> 포인트 서비스 처리 -> 인프라/배포 준비 -> 스키마/테스트 데이터

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
```
- `coupon-service/src/main/resources/coupon_schema.sql`
```sql
CREATE TABLE IF NOT EXISTS coupon (
                                      coupon_number VARCHAR(255) PRIMARY KEY,
    status VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expired_at TIMESTAMP NOT NULL
    );

TRUNCATE TABLE coupon;

INSERT INTO coupon (coupon_number, status, issued_at, expired_at)
VALUES (
           'C-001',
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
           'CPN-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);
```
- `point-service/src/main/resources/point_schema.sql`
```sql
CREATE TABLE IF NOT EXISTS point (
                                     point_number VARCHAR(255) PRIMARY KEY,
    status VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expired_at TIMESTAMP NOT NULL
    );

TRUNCATE TABLE point;

INSERT INTO point (point_number, status, issued_at, expired_at)
VALUES (
           'P-001',
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
           'PNT-001',
           'AVAILABLE',
           CURRENT_TIMESTAMP,
           DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
       )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         issued_at = VALUES(issued_at),
                         expired_at = VALUES(expired_at);
```
- `bin_k8s/04_portforward_order_orchestrator.sh`
```bash
#!/usr/bin/env bash
# bash를 엄격한 모드로 실행하는 옵션 설정
#  -e: 어떤 명령이 실패(비정상 종료)하면 즉시 스크립트 종료
#  -u: 선언되지 않은 변수를 사용하면 에러로 처리
#  -o pipefail: 파이프라인에서 앞 단계가 실패해도 전체 실패로 인식 (기본은 마지막 명령만 체크)
set -euo pipefail

echo "order-orchestrator rollout 체크 중..."
kubectl -n msa rollout status deployment/order-orchestrator
kubectl port-forward -n msa svc/order-orchestrator 8099:8099
```
- `order-orchestrator/src/main/resources/orderOS_cleanup.sql`
```sql
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM order_item;
DELETE FROM outbox_message;
DELETE FROM order_saga;

SET FOREIGN_KEY_CHECKS = 1;
```
- `order-orchestrator/src/main/resources/orderOS_schema.sql`
```sql
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM order_item;
DELETE FROM outbox_message;
DELETE FROM order_saga;

SET FOREIGN_KEY_CHECKS = 1;
```

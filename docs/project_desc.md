# Order Saga System - 프로젝트 구조 요약

이 문서는 멀티모듈 프로젝트의 역할과 흐름을 사람이 읽기 쉬운 형태로 정리한 것이다.

## 1) 모듈 구성

- `order-orchestrator`: 주문 오케스트레이터 MSA (주문 생성, 사가 상태 관리, 이벤트 발행)
- `order-saga-consumer`: Kafka 이벤트 컨슈머 (사가 이벤트 처리, 확정/보상 호출)
- `coupon-service`: 쿠폰 MSA (예약/확정/보상)
- `point-service`: 포인트 MSA (예약/확정/보상)
- `account`: 계정 예제 MSA (입금/출금/조회)
- `common`: 공통 타입 (MSAStatus, OrderSagaStatus, 공통 API 응답 등)

## 2) 핵심 기능 역할

### 주문 오케스트레이터 (order-orchestrator)
- 주문 생성 API 제공: `POST /api/v1/orders`
- 쿠폰/포인트 예약을 병렬 호출
- 예약 성공 시 `OrderSagaStatus.Reserved` 업데이트 및 Kafka 이벤트 발행
- 예약 실패 시 `OrderSagaStatus.Compensating` 업데이트 및 Kafka 실패 이벤트 발행
- Outbox 패턴으로 상태 기록과 이벤트 발행을 연계

### 쿠폰/포인트 MSA (coupon-service / point-service)
- 예약/확정/보상 API 제공
- 예약: `POST /api/v1/{coupons|points}/reserve`
- 확정: `POST /api/v1/{coupons|points}/confirm`
- 보상: `POST /api/v1/{coupons|points}/compensate`

### 주문 사가 컨슈머 (order-saga-consumer)
- Kafka에서 사가 이벤트 수신
- 이벤트의 `orderId`, `status`를 읽어 order_saga 조회
- 상태에 따라 쿠폰/포인트 확정 또는 보상 호출
- 성공 시 outbox와 order_saga 상태를 Completed/Compensated로 갱신

### 계정 MSA (account)
- 계정 생성, 입금/출금/조회 API 제공
- 주문 사가 흐름과는 독립적인 예제 서비스

## 3) 이벤트/상태 흐름

1. 주문 생성 요청 → 주문 오케스트레이터가 주문/사가/아웃박스 생성
2. 쿠폰/포인트 예약 호출
3. 성공: `Reserved` 이벤트 발행, 실패: `Compensating` 이벤트 발행
4. 컨슈머가 이벤트 수신 후 확정/보상 수행
5. 성공 시 `Completed` 또는 `Compensated`로 상태 전이

## 4) 아키텍처 다이어그램

이미지 파일:
 ![Architecture Diagram](/docs/architecture_diagram.svg.jpg)


이미지는 아래 흐름을 시각화한다.
- Client → order-orchestrator (주문 생성)
- order-orchestrator → coupon/point (reserve)
- order-orchestrator → Kafka (saga event publish)
- order-saga-consumer ← Kafka (consume)
- order-saga-consumer → coupon/point (confirm/compensate)
- Client → account (account APIs)

## 5) 실행 환경 요약

- 로컬 개발: `bin` 스크립트 사용 (로컬 MSA + K8s MySQL/Kafka 포트포워드)
- K8s dev 테스트: `bin` 스크립트 사용 (빌드/배포/포트포워드)
- 기존 K8s 스크립트: `bin_k8s` 유지

# Codex 작업 로그

아래는 현재까지 대화에서 확정된 작업 내용을 순서대로 요약한 로그이다.

## 1) OrderSagaConsumer 모듈 추가 및 기본 컨슈머 구성
- 새 모듈 `order-saga-consumer` 추가(기존 `OrderSagaConsumer`에서 이름 변경).
- `OSC_application.yaml` 구성: `order-orchestrator`의 `orderOS_application.yaml` 내용을 복사하고 `sql.init` 제거.
- Kafka 컨슈머 생성: 토픽 메시지 payload 출력 포맷을 `KafkaTopicPrinter` 스타일로 출력.
- `OrderSagaConsumerApplication` 실행 시 `spring.profiles.active` 미지정이면 `test`가 기본이 되도록 설정.
- 실행 스크립트 추가: `bin_k8s/07_dev_consumer.sh`, `bin_k8s/07_test_consumer.sh`.

## 2) order-orchestrator: order_saga에 point_number 추가
- 도메인/엔티티/저장 로직에 `pointNumber` 추가.
- `CreateOrderService`에서 `pointNumber` 저장하도록 수정.
- 테스트 보강: 서비스/통합 테스트에서 `pointNumber` 저장 검증 추가.

## 3) order-saga-consumer: 헥사고날 아키텍처 적용
- Kafka 이벤트를 단건 순차 처리하도록 전환.
- 이벤트 payload에서 `orderId`, `status` 추출.
- `order_saga` 조회 후 `coupon_number`, `point_number`를 보기 좋게 출력.
- 포트/유스케이스/어댑터/도메인 모델 분리.

## 4) coupon-service: confirm/compensate API 추가 및 리팩토링
- `confirm` API 추가: `RESERVED -> USED`.
- `compensate` API 추가: `RESERVED -> COMPENSATED`.
- reserve/confirm 로직 공통화.
- compensate는 예약 실패 시 예외 없이 no-op 처리(쿠폰 없거나 RESERVED가 아니면 무시).
- HTTP 테스트 추가: `01_couponServiceTest.http`.
- 테스트 확장: 단위/Mock/통합 테스트에 confirm/compensate 시나리오 추가.

## 5) point-service: confirm/compensate API 추가 (coupon-service와 동일 방식)
- `confirm` API 추가: `RESERVED -> USED`.
- `compensate` API 추가: `RESERVED -> COMPENSATED`.
- reserve/confirm 로직 공통화.
- compensate는 예약 실패 시 예외 없이 no-op 처리.
- HTTP 테스트 추가: `01_pointServiceTest.http`.
- 테스트 확장: 단위/Mock/통합 테스트에 confirm/compensate 시나리오 추가.

## point API에 추가
› coupon-service에 추가한 confirm, compansate API를 point-service에도 동일한 방식으로 추가해 줘.


## 6) Logging Mode 변경
- 사용자 요청: 지금부터 모든 발화를 `docs/codex_log.md`에 저장.
- Codex 응답: 매 요청/응답마다 즉시 append 방식으로 기록 진행.

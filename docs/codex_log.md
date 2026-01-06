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

## 6) Logging Mode 변경
- 사용자 요청: 지금부터 모든 발화를 `docs/codex_log.md`에 저장.
- Codex 응답: 매 요청/응답마다 즉시 append 방식으로 기록 진행.

## 7) 보상 처리 예외 정책 변경
- coupon-service/point-service의 compensate 메서드에서 status가 USED인 경우 예외를 반환하도록 변경.
- RESERVED가 아닌 경우(AVAILABLE 등)에는 기존대로 no-op 유지.
- 관련 단위/Mock 테스트에 USED 케이스 추가.

## 8) 보상 완료 상태 변경 요청
- 사용자 요청: 포인트와 쿠폰의 보상 처리가 완료되면 point, coupon의 status는 AVAILABLE 상태가 되어야 함.
- Codex 응답: 보상 처리 완료 시 coupon/point 상태를 AVAILABLE로 변경하도록 서비스/컨트롤러/테스트/HTTP 예시를 업데이트.

## 9) USED 보상 오류 메시지 명시 요청
- 사용자 요청: 이미 USED가 된 포인트를 보상하려고 하면 500 Internal Server Error가 발생하니, 서비스에서 담아주는 에러 메시지로 더 명시적으로 리턴 요청.
- Codex 응답: point-service 보상 요청의 USED 상태 오류가 500이 아닌 명시적 에러로 반환되도록 전역 예외 처리기를 추가하고, 통합 테스트 기대 상태를 CONFLICT로 변경.

## 10) reserve/confirm 응답 메시지 변경 요청
- 사용자 요청: reserve, confirm API도 응답 메시지를 서비스에서 발생한 메시지로 변경하고, point, coupon 양쪽 모두 작업 요청.

## 11) 작업 내용 확인 요청
- 사용자 요청: 이 창이 닫히기 전에 작업했던 내용을 확인해줘.
- Codex 응답: `docs/codex_log.md`에 정리된 작업 기록과 git 상태를 안내.

## 12) 모든 발화 로깅 요청
- 사용자 요청: 지금부터 모든 발화를 `docs/codex_log.md`에 저장해줘. 발화의 순번은 `## xx) 제목` 형식으로 기록 요청.
- Codex 응답: 요청대로 `docs/codex_log.md`에 모든 발화를 바로 기록하겠다고 응답.


## 13) 최근 커밋 요약 요청
- 사용자 요청: 가장 최근에 커밋된 내용을 분석해서 요약해줘.
- Codex 응답: 최근 커밋의 주요 변경 사항을 요약 제공.

## 14) reserve/confirm 예외 처리 확대 요청
- 사용자 요청: 동일한 작업을 reserve*, confirm* 메서드에도 적용 요청(쿠폰/포인트 서비스 모두).
- Codex 응답: coupon-service에 전역 예외 처리기를 추가하고 reserve 실패 테스트를 CONFLICT로 조정해 reserve/confirm 오류도 명시적 응답으로 처리.

## 15) confirm 멱등 처리 요청
- 사용자 요청: 이미 confirm한 쿠폰/포인트를 다시 confirm하면 오류 대신 성공 처리(멱등) 요청.
- Codex 응답: confirm 시 USED 상태면 no-op으로 처리하도록 서비스 로직을 변경하고 관련 테스트를 추가.

1
## 17) order-orchestrator K8s 테스트 500 에러 문의
- 사용자 요청: `00_init_k8s.sh` 실행 후 `01_orderOrchestratorK8sTest.http`의 주문 생성 요청에서 500 에러 발생.
- Codex 응답: 원인 파악을 위해 관련 서비스/파드 로그와 상태 점검 진행 예정.

## 18) OrderSagaConsumer 로컬 처리 작업 리스트 요청
- 사용자 요청: k8s 배포 후 로컬 OrderSagaConsumerApplication(dev)에서 성공/실패 메시지를 처리하기 위한 작업 리스트 요청.
- Codex 응답: 실행/연결/토픽/포트포워딩/프로파일 설정 중심의 작업 리스트 제공.

## 19) OSC 로컬-쿠버네티스 연결 심화 분석 요청
- 사용자 요청: 로컬 OSC에서 K8s 메시지 처리 위한 포트포워딩과 새 프로파일 추가 등 상세 분석 요청.
- Codex 응답: Kafka/쿠폰/포인트/오케스트레이터 포트포워딩과 OSC_application.yaml 신규 프로파일 구성 및 실행 절차를 정리.

## 20) K8s 포트포워딩 스크립트 확장 요청
- 사용자 요청: `03_deploy_all.sh`에 포트포워딩 추가, `_03_kill_portforward.sh`에 종료 처리 추가.
- Codex 응답: Kafka/쿠폰/포인트/오케스트레이터 포트포워딩 및 kill 로직을 스크립트에 반영.

## 21) bin_k8s 쉘 점검 요청
- 사용자 요청: bin_k8s 하위 쉘을 점검해 수정 필요 사항 확인.
- Codex 응답: 포트포워딩 중복/순서/종료 스크립트 불일치 등 개선 포인트를 정리.

## 22) Kafka 포트포워딩 순서 이슈 위치 문의
- 사용자 요청: 03_deploy_all.sh에 Kafka 포트포워딩 추가로 인한 순서 이슈의 위치를 질문.
- Codex 응답: 00_init_k8s.sh의 실행 순서와 03_deploy_all.sh 내 포워딩 라인을 근거로 설명.

## 23) OSC k8s-local 프로파일 추가
- 사용자 요청: OSC_application.yaml에 로컬→K8s 전용 프로파일 추가.
- Codex 응답: k8s-local 프로파일을 추가해 로컬 포트포워딩 기반 설정을 반영.

## 24) _03_stop_msa.sh 포트포워드 종료 확장
- 사용자 요청: coupon/point 포트포워드 종료 추가.
- Codex 응답: 8081/8082 포트포워드 종료 로직을 추가.

## 25) K8s 테스트 후 로컬 OSC 미소비 원인 분석 요청
- 사용자 요청: 00_init_k8s.sh, 07_dev_consumer.sh 실행 후 API 호출했지만 메시지 소비가 안 됨. 원인 분석 요청.
- Codex 응답: 프로파일/포트포워딩/Kafka 접속 설정 불일치 가능성을 지적하고 점검 절차 제시.

## 26) ### Consumer의 SAGA 개발 ###
ProcessOrderSagaEventService process 메소드에서 가장 하단에 SAGA 처리 로직을 추가할꺼야.
status가 Reserved이면
- info.couponNumber에 값이 존재하면 coupon-service confirm API를
- info.pointNumber에 값이 존재하면 point-service confirm API를 각각 호출해줘.

- status가 Compensating이면 
- info.couponNumber에 값이 존재하면 coupon-service compensate API를
- info.pointNumber에 값이 존재하면 point-service compensate API를 각각 호출해줘.

- 각 MSA 호출 결과에 따라 outbox_message의 각 서비스(coupon_status, point_status)를 업데이트 하고, 
- 필요한 MSA 호출이 모두 성공하면 outbox_message의 saga_status와 order_saga의 status를 confirm이 성공한 경우에는 Completed로
- compensate를 성공한 경우에는 Compensated로 업데이트 해줘.

## 27) K8s 포트포워딩 오류 및 outbox order_status 갱신
- 사용자 요청: 05_msa_portforward.sh 실행 시 로그 파일이 read-only 경로로 떨어지는 오류 수정 요청.
- Codex 응답: 스크립트에 ROOT_DIR 정의를 추가해 로그가 프로젝트 루트에 기록되도록 수정.
- 사용자 요청: outbox_message의 saga_status가 Completed/Compensated가 될 때 order_status도 동일하게 업데이트.
- Codex 응답: OutboxMessageStatusJdbcAdapter.updateSagaStatus에서 saga_status에 따라 order_status를 함께 갱신하도록 반영.

## 29) 리팩터링
High Priority
- 상태 모델 중복: order-orchestrator/src/main/java/com/example/orderorchestrator/domain/model/status/MSAStatus.java, order-saga-consumer/src/main/java/com/example/ordersagaconsumer/domain/model/status/MSAStatus.java 및 OrderSagaStatus가 모듈마다 중복 → common으로 이동해 단일화 필요.

Medium Priority
- WebClient 예외 처리 패턴: CouponServiceClient, PointServiceClient에서 공통 예외 처리/타임아웃/리트라이가 중복(order-saga-consumer/src/main/java/com/example/ordersagaconsumer/adapter/out/webclient/*.java) → 공통 클라이언트/헬퍼로 분리 권장.
- SAGA 상태 업데이트 메서드 구조: 상태별 업데이트가 여러 메서드로 분리됐지만 호출 규칙이 산발적(order-saga-consumer/src/main/java/com/example/ordersagaconsumer/application/service/ProcessOrderSagaEventService.java, order-saga-consumer/src/main/java/com/example/ordersagaconsumer/adapter/out/persistence/OutboxMessageStatusJdbcAdapter.java) → 상태 전이 정책을 한 곳에서 관리하도록 묶는 리팩토링 고려.

## 30) 리팩터링 반영: 공통 상태 모델, WebClient 헬퍼, Saga 전이 서비스
- Codex 작업: MSAStatus/OrderSagaStatus를 common 모듈로 이동하고, 각 모듈의 import를 공통 패키지로 교체.
- Codex 작업: order-saga-consumer에 WebClient 공통 헬퍼(ServiceClientSupport) 추가 및 Coupon/Point 클라이언트 공통화(타임아웃/리트라이 설정).
- Codex 작업: SagaStatusTransitionService로 saga 상태 전이 업데이트를 한 곳에 집중하고 ProcessOrderSagaEventService에서 호출하도록 정리.

### 28) updateSagaStatus 메서드 리팩터링
OutboxMessageStatusJdbcAdapter.java 에서 하나의 메서드가 여러 역할을 하고 있는데, updateSagaCompetedStatus, updateSagaCompensatedStatus 처럼 각각의 메서드로 리팩토링 하자.
    @Override
    public void updateSagaStatus(String orderId, OrderSagaStatus status) {
        if (status == OrderSagaStatus.Completed) {
        //생략...
            return;
        }

        if (status == OrderSagaStatus.Compensated) {
            //생략...
            return;
        }

        //생략...
    }

### 29) 리팩터링
High Priority
- 상태 모델 중복: order-orchestrator/src/main/java/com/example/orderorchestrator/domain/model/status/MSAStatus.java, order-saga-consumer/src/main/java/com/example/ordersagaconsumer/domain/model/status/MSAStatus.java 
및 OrderSagaStatus가 모듈마다 중복 → common으로 이동해 단일화 필요. 

Medium Priority
- WebClient 예외 처리 패턴: CouponServiceClient, PointServiceClient에서 공통 예외 처리/타임아웃/리트라이가 중복(order-saga-consumer/src/main/java/com/example/ordersagaconsumer/adapter/out/webclient/*.java) → 공통 클라이언트/헬퍼로 분리 권장.
- SAGA 상태 업데이트 메서드 구조: 상태별 업데이트가 여러 메서드로 분리됐지만 호출 규칙이 산발적(order-saga-consumer/src/main/java/com/example/
  ordersagaconsumer/application/service/ProcessOrderSagaEventService.java, order-saga-consumer/src/main/java/com/example/ordersagaconsumer/adapter/
  out/persistence/OutboxMessageStatusJdbcAdapter.java) → 상태 전이 정책을 한 곳에서 관리하도록 묶는 리팩토링 고려.


- 스크립트 중복/불일치: 포트포워딩/kill 로직이 여러 스크립트에 분산(bin_k8s/_03_kill_portforward.sh, bin_k8s/_03_stop_msa.sh) → 공통 함수/스크립트
  로 통합 권장.


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

## 30) Consumer 빌드 및 K8s 배포
msa namespace에 실제로 배포하는 것으로 스크립트를 구성해.
- ORDER_ORCH_DB_PASSWORD 값은 기존 point, coupon MSA처럼 K8s YAML로 구성해줘.
- 빌드 구성을 위해 order-saga-consumer 모듈 루트에 dockerfile을 추가해 주고 
- order-saga-consumer/scripts/deploy_k8s.sh 를 만들어줘.
- 프로젝트 루트/bin_test/ 에 03_*, _03_*로 배포 및 프로세스 시작/종료 쉘도 만들어 줘.
- 물론 새로 만든 03_* 은 consumer까지 K8s 안에서 실행이 되어야 하고, 
- 마지막에 실행하는 Consumer의 system.out.log가 출력이 되도록 실행 스크립트를 만들어줘

## 31)  circuit-breaker를 구성안 검토
circuit-breaker를 구성해서 MSA(order, point)의 장애에 circuit-breaker가 동작하도록 하려고 해. 
Istio를 비롯해서 현재 K8s(dev) 구성에서 circuit-breaker를 구성해서 장애 테스트를 쉽게 할 수 있는 방안을 몇 개 제안해 줘.

[요구사항]
- circuit breaker 구성
- order, point, coupon의 timeout을 x초(yaml 설정, 5초)로 제안
- 각 MSA 서비스(order, point, coupon)에서 timeout이 연속으로 x회(yaml 설정, 3회) 발생 시 circuit 차단
- 이후 10초 이후에 트래픽을 흘려서 성공하면 즉시 circuit 재연결
- timeout을 테스트 할 수 있도록 테스트 방법 제공

[테스트]
- order를 시작으로 coupon, point 정상 연결됨 1회
- 이후 order는 연결되나 coupon API 3번 연속 timeout 발생
- circuit 차단 확인
- 5초 후 order 재호출, order -> coupon 호출 차단됨
- 11초 후 order 재호출 order -> coupon 호출 성공됨

위 요구사항과 테스트 시나리오를 바탕으로 현재 프로젝트에 어떻게 반영하는게 좋을지 제안해줘.

--> 답변
        • 다음처럼 “지금 K8s(dev) 구성에서 빠르게 테스트 가능한 circuit‑breaker” 옵션 3가지를 제안할게. 공통 파라미터는 요구사항대로 timeout=5s, 연속 실패 3회 차단, 10s 후 half‑open 트래픽 허용으로 맞춘다.
        
        Option 1: Istio(Envoy) 기반 – 가장 K8s‑네이티브, 중앙 제어
        
        - 장점: 앱 수정 없이 적용, dev 환경에 가장 잘 맞음.
          - 핵심: DestinationRule + VirtualService로 timeout/CB 설정.
        
        예시(Concept)
        
        - VirtualService에서 timeout: 5s
          - DestinationRule에서 outlierDetection
              - consecutive5xxErrors: 3 또는 consecutiveGatewayErrors: 3
              - baseEjectionTime: 10s
              - interval: 5s (헬스 체크)
              - maxEjectionPercent: 100
        
        타임아웃을 5s로 하고, 3회 연속 5xx(또는 gateway error) 발생 시 10s 동안 ejection(차단) → 10s 후 자동 복귀 시도. 성공하면 즉시 회복.
        
        테스트 방법
        
        1. coupon 서비스에 delay 주입(예: 응답 6초 이상 지연) → timeout 유도
           2. order → coupon 3회 호출로 timeout 발생
           3. 4번째부터는 circuit 차단 확인(즉시 실패)
           4. 5초 후 재호출 시 still 차단
           5. 11초 후 재호출 시 정상 회복 확인
        
        ———
        
        Option 2: Spring Cloud Resilience4j (앱 레벨)
        
        - 장점: 서비스별 세밀한 제어, 앱 로직에 맞춤형 회복 처리 가능.
          - 단점: 모든 서비스에 코드/설정 필요.
        
        개념 설정(예)
        
        - timeout: 5s
          - slidingWindowType: COUNT_BASED
          - slidingWindowSize: 3
          - minimumNumberOfCalls: 3
          - failureRateThreshold: 100 (3번 모두 실패시 차단)
          - waitDurationInOpenState: 10s
          - permittedNumberOfCallsInHalfOpenState: 1 (성공 시 즉시 복귀)
        
        테스트 방법
        
        - coupon 응답 지연(6초) → 3회 타임아웃 → open
          - 5초 후 재호출 실패 확인
          - 11초 후 재호출 성공 시 즉시 close
        
        ———
        
        Option 3: Istio + Fault Injection 조합 (테스트 특화)
        
        - 장점: dev에서 테스트 자동화에 유리.
          - 구성: 실제 CB는 Istio로 두고, VirtualService에 fault injection 추가해 timeout을 재현.
          - 예: 100% delay 6s로 coupon에 적용 → order에서 3회 연속 timeout 유도
          - 이후 fault 제거하고 11초 후 정상 복귀 확인
        
        ———
        
        ## 프로젝트 반영 제안 (현 상황 기준)
        
        dev 환경에서 손쉽게 장애 테스트가 필요하니 **Option 1(Istio)**을 기본으로 두고, Option 3로 테스트 시나리오를 자동화하는 구성이 가장 간결함.
        order/point/coupon 모두 동일 파라미터 적용: timeout=5s, 3회 연속 timeout 시 10s 차단, 이후 성공 시 즉시 복귀.
        
        필요하면 다음 단계로 구체 YAML(각 서비스별 DestinationRule/VirtualService)까지 바로 작성해 줄게.
        원하면 어느 옵션으로 갈지 번호로 알려줘.


## 32)  circuit-breaker를 구성
› dev 환경에서 손쉽게 장애 테스트가 필요하니 **Option 1(Istio)**을 기본으로 두고, Option 3로 테스트 시나리오를 자동화하는 구성이 가장 간결함.
order/point/coupon 모두 동일 파라미터 적용: timeout=5s, 3회 연속 timeout 시 10s 차단, 이후 성공 시 즉시 복귀.

필요하면 다음 단계로 구체 YAML(각 서비스별 DestinationRule/VirtualService)까지 바로 작성해


## 33) circuit-breaker 배포 및 테스트 환경 업데이트
circuit-breaker 실행 환경 업데이트

#Apply (base CB)
kubectl label namespace msa istio-injection=enabled --overwrite
kubectl -n msa apply -f bin_k8s/istio/circuit-breaker.yaml
는

- 03_deploy_all.sh
- 02_prepare_k8s_order_orchestrator_test.sh
을 비롯해 아래 경로의 쉘에서 k8s 배포 및 테스트와 관련된 부분에 방금 제안한 istio 배포, 재기동 등이 적용되도록 sh을 수정해 줘.
/bin_k8s
/bin_test

그리고, 테스트 방식으로 제안한 아래 부분도 메뉴얼이 아닌 자동으로 수행할 수 있는 방법을 제안해줘. 

Fault injection on coupon (timeout 유도)

kubectl -n msa apply -f bin_k8s/istio/coupon-fault-delay.yaml

Test scenario (요구사항 그대로)

1. order → coupon/point 정상 1회

kubectl -n msa port-forward svc/order-orchestrator 8099:8099
curl -X POST http://localhost:8099/api/v1/orders \
-H "Content-Type: application/json" \
-d '{"couponNumber":"CPN-INT-BOTH-001","pointNumber":"PNT-INT-BOTH-001","paymentNumber":"PAY-001","paymentAmount":15000,"orderItems":[{"itemNumber":"ITEM-001","quantity":2}]}'

2. coupon 3회 연속 timeout 발생 (fault delay 6s + timeout 5s)

for i in 1 2 3; do
curl -X POST http://localhost:8099/api/v1/orders \
-H "Content-Type: application/json" \
-d '{"couponNumber":"CPN-INT-BOTH-001","pointNumber":"PNT-INT-BOTH-001","paymentNumber":"PAY-001","paymentAmount":15000,"orderItems":[{"itemNumber":"ITEM-001","quantity":2}]}'
done

3. fault 제거(서비스 정상화)

kubectl -n msa apply -f bin_k8s/istio/circuit-breaker.yaml

4. 5초 후 재호출 → circuit 차단 확인
5. 11초 후 재호출 → circuit 복귀 확인

# 08. Kafka 기반 EDA 구성

## 목표
- 토픽 생성, 이벤트 발행/소비, 테스트 구조를 이해한다.

## 스토리라인
- 사가를 안정적으로 연결하기 위해 이벤트 흐름을 검증.

## 관련 커밋
- `499aff6`, `10270ba`, `9a613a8`, `aeceecc`, `9aa633c`

## 커밋 변경 요약
| 커밋 | 주요 변경 요약 | 체크아웃 |
| --- | --- | --- |
| `499aff6` | Kafka 브로커 구성 및 포트 포워드 | `git checkout 499aff6` |
| `10270ba` | 토픽 생성, 기존 토픽 삭제, 이벤트(토픽) 발행, 브로커 접속 테스트, 이벤트 소비 테스트 추가 | `git checkout 10270ba` |
| `9a613a8` | 토픽 생성, 기존 토픽 삭제, 이벤트(토픽) 발행, 브로커 접속 테스트, 이벤트 소비 테스트 추가 | `git checkout 9a613a8` |
| `aeceecc` | 테스트 토픽 분리 & 토픽 발행 테스트 OrderSagaEventPublishIntegrationTest | `git checkout aeceecc` |
| `9aa633c` | 통합 테스트 카프카 토픽 로그 추가 | `git checkout 9aa633c` |

## 핵심 개념
- 토픽 관리, 테스트 환경 분리
- 이벤트 발행 테스트

## 기술/기능/프로세스
- 기술: Kafka, 토픽 관리, 이벤트 발행/소비 테스트
- 기능: 토픽 생성/삭제, 발행/소비 검증
- MSA: 서비스 간 비동기 연결
- EDA: 이벤트 토픽 분리와 테스트 전략
## 데모/실습
- 카프카 테스트 코드: `order-orchestrator/src/test/java/.../adapter/out/kafka/*`

## 커밋 상세
### 499aff6 Kafka 브로커 구성 및 포트 포워드
- 주요 변경: Kafka 브로커 구성 및 포트 포워드
- 핵심 코드: `bin_k8s/kafka.yaml`
```yaml
apiVersion: v1
kind: Service
metadata:
  name: kafka
  namespace: msa
spec:
//--- 생략 ...
```
- 설명: Kafka/Consumer 배포 설정을 추가해 실행 환경을 고정한다.

### 10270ba 토픽 생성, 기존 토픽 삭제, 이벤트(토픽) 발행, 브로커 접속 테스트, 이벤트 소비 테스트 추가
- 주요 변경: 토픽 생성, 기존 토픽 삭제, 이벤트(토픽) 발행, 브로커 접속 테스트, 이벤트 소비 테스트 추가
- 핵심 코드: `bin_k8s/06_deploy_kafka.sh`
```bash
//--- 생략 ...
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="${ROOT_DIR}/kafka-port-forward.pid"

kubectl -n msa apply -f "${ROOT_DIR}/kafka.yaml"
kubectl -n msa rollout status deployment/kafka

if [[ -f "${PID_FILE}" ]]; then
//--- 생략 ...
```
- 설명: 실행/테스트 스크립트를 통해 분산 시나리오를 재현한다.

### 9a613a8 토픽 생성, 기존 토픽 삭제, 이벤트(토픽) 발행, 브로커 접속 테스트, 이벤트 소비 테스트 추가
- 주요 변경: 토픽 생성, 기존 토픽 삭제, 이벤트(토픽) 발행, 브로커 접속 테스트, 이벤트 소비 테스트 추가
- 핵심 코드: `bin_k8s/06_deploy_kafka.sh`
```bash
//--- 생략 ...
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="${ROOT_DIR}/kafka-port-forward.pid"

kubectl -n msa apply -f "${ROOT_DIR}/kafka.yaml"
kubectl -n msa rollout status deployment/kafka

if [[ -f "${PID_FILE}" ]]; then
//--- 생략 ...
```
- 설명: 실행/테스트 스크립트를 통해 분산 시나리오를 재현한다.

### aeceecc 테스트 토픽 분리 & 토픽 발행 테스트 OrderSagaEventPublishIntegrationTest
- 주요 변경: 테스트 토픽 분리 & 토픽 발행 테스트 OrderSagaEventPublishIntegrationTest
- 핵심 코드: `order-orchestrator/src/test/java/com/example/orderorchestrator/adapter/out/kafka/OrderSagaEventPublishIntegrationTest.java`
```java
class OrderSagaEventPublishIntegrationTest {
//--- 생략 ...
                .send(topic, key, payload)
                .get(10, TimeUnit.SECONDS);

        RecordMetadata metadata = result.getRecordMetadata();
        assertThat(metadata).isNotNull();
        assertThat(metadata.topic()).isEqualTo(topic);
    }

    private void assertKafkaAvailable() throws Exception {
//--- 생략 ...
}
```
- 설명: Kafka 이벤트를 발행해 서비스 간 비동기 연계를 구성한다.

### 9aa633c 통합 테스트 카프카 토픽 로그 추가
- 주요 변경: 통합 테스트 카프카 토픽 로그 추가
- 핵심 코드: `order-orchestrator/src/test/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationIntegrationTest.java`
```java
class OrderOrchestrationIntegrationTest {
//--- 생략 ...
            ServiceContext context = startService(
                    PointServiceApplication.class,
                    "point_application",
                    "point_schema.sql",
                    8082,
                    "point"
            );
            pointContext = context.context();
            pointPort = context.port();
        }

        registry.add("external.point.base-url", () -> "http://localhost:" + pointPort);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderSagaJpaRepository orderSagaJpaRepository;

    @Autowired
    private OutboxMessageJpaRepository outboxMessageJpaRepository;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    //@AfterEach
    void tearDown() {
        outboxMessageJpaRepository.deleteAll();
        orderSagaJpaRepository.deleteAll();
    }

    // 쿠폰과 포인트 모두 예약 가능한 경우
    @Test
    void createOrder_withCouponAndPoint_shouldPersistOrderSaga_and_OutboxMessage() {
        // given: 주문 생성 요청 바디
        Map<String, Object> requestBody = Map.of(
                "couponNumber", "CPN-INT-BOTH-001",
                "pointNumber", "PNT-INT-BOTH-001",
                "paymentNumber", "PAY-001",
                "paymentAmount", 35000L,
                "orderItems", List.of(
                        Map.of("itemNumber", "ITEM-001", "quantity", 2),
                        Map.of("itemNumber", "ITEM-002", "quantity", 1)
                )
        );

        assertOrderCreated(requestBody, MSAStatus.Reserved, MSAStatus.Reserved, OrderSagaStatus.Reserved);
    }

    // 쿠폰만 사용하는 경우
    @Test
    void createOrder_withCouponOnly_shouldPersistOrderSaga_and_OutboxMessage() {
        Map<String, Object> requestBody = Map.of(
                "couponNumber", "CPN-INT-ONLY-001",
                "paymentNumber", "PAY-001",
                "paymentAmount", 35000L,
                "orderItems", List.of(
                        Map.of("itemNumber", "ITEM-001", "quantity", 2),
                        Map.of("itemNumber", "ITEM-002", "quantity", 1)
                )
        );

        assertOrderCreated(requestBody, MSAStatus.Reserved, MSAStatus.NotUsed, OrderSagaStatus.Reserved);
    }

    // 포인트만 사용하는 경우
    @Test
    void createOrder_withPointOnly_shouldPersistOrderSaga_and_OutboxMessage() {
        Map<String, Object> requestBody = Map.of(
                "pointNumber", "PNT-INT-ONLY-001",
                "paymentNumber", "PAY-001",
                "paymentAmount", 35000L,
                "orderItems", List.of(
                        Map.of("itemNumber", "ITEM-001", "quantity", 2),
                        Map.of("itemNumber", "ITEM-002", "quantity", 1)
                )
        );

        assertOrderCreated(requestBody, MSAStatus.NotUsed, MSAStatus.Reserved, OrderSagaStatus.Reserved);
    }

    // 쿠폰/포인트 없이 주문하는 경우
    @Test
    void createOrder_withoutCouponAndPoint_shouldPersistOrderSaga_and_OutboxMessage() {
        Map<String, Object> requestBody = Map.of(
                "paymentNumber", "PAY-001",
                "paymentAmount", 35000L,
                "orderItems", List.of(
                        Map.of("itemNumber", "ITEM-001", "quantity", 2),
                        Map.of("itemNumber", "ITEM-002", "quantity", 1)
                )
        );

        assertOrderCreated(requestBody, MSAStatus.NotUsed, MSAStatus.NotUsed, OrderSagaStatus.Reserved);
    }

    // 쿠폰은 이미 예약되어 실패하고, 포인트는 예약 가능한 경우
    @Test
    void createOrder_withReservedCouponAndAvailablePoint_shouldMarkCouponFailedAndPointReserved() {
        Map<String, Object> requestBody = Map.of(
                "couponNumber", "CPN-INT-BOTH-RESERVED-001",
                "pointNumber", "PNT-INT-BOTH-AVAILABLE-001",
                "paymentNumber", "PAY-001",
                "paymentAmount", 35000L,
                "orderItems", List.of(
                        Map.of("itemNumber", "ITEM-001", "quantity", 2),
                        Map.of("itemNumber", "ITEM-002", "quantity", 1)
                )
        );

        assertOrderCreatedWithExternalFailure(requestBody, MSAStatus.Failed, MSAStatus.Reserved, OrderSagaStatus.Compensating);
    }

    private void assertOrderCreated(
            Map<String, Object> requestBody,
            MSAStatus expectedCouponStatus,
            MSAStatus expectedPointStatus,
            OrderSagaStatus expectedSagaStatus
    ) {
        HttpEntity<Map<String, Object>> httpEntity = buildHttpEntity(requestBody);

        // when: /api/v1/orders 호출
        ResponseEntity<CreateOrderResponse> response = restTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                httpEntity,
                CreateOrderResponse.class
        );

        // then: HTTP 응답 검증
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        CreateOrderResponse body = response.getBody();
        String orderId = body.orderId();
        String sagaId = body.sagaId();
        String status = body.status();

        assertThat(orderId).isNotBlank();
        assertThat(sagaId).isNotBlank();
        assertThat(status).isEqualTo(OrderSagaStatus.InProgress.name());

        // 그리고 H2 DB에 order_saga, outbox_message 가 잘 들어갔는지 확인

        // 1) order_saga 테이블
        Optional<OrderSagaJpaEntity> sagaOpt = orderSagaJpaRepository.findByOrderId(orderId);
        assertThat(sagaOpt).isPresent();

        OrderSagaJpaEntity sagaEntity = sagaOpt.get();
        assertOrderSaga(sagaEntity, orderId, sagaId, expectedSagaStatus);
        assertOutbox(orderId, expectedCouponStatus, expectedPointStatus, expectedSagaStatus, true);
    }

    private void assertOrderCreatedWithExternalFailure(
            Map<String, Object> requestBody,
            MSAStatus expectedCouponStatus,
            MSAStatus expectedPointStatus,
            OrderSagaStatus expectedSagaStatus
    ) {
        HttpEntity<Map<String, Object>> httpEntity = buildHttpEntity(requestBody);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                httpEntity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        OrderSagaJpaEntity sagaEntity = findLatestSaga();
        String orderId = sagaEntity.getOrderId();

        assertOrderSaga(sagaEntity, orderId, sagaEntity.getSagaId(), expectedSagaStatus);
        assertOutbox(orderId, expectedCouponStatus, expectedPointStatus, expectedSagaStatus, false);
    }

    private HttpEntity<Map<String, Object>> buildHttpEntity(Map<String, Object> requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(requestBody, headers);
    }

    private OrderSagaJpaEntity findLatestSaga() {
        List<OrderSagaJpaEntity> sagas = orderSagaJpaRepository.findAll();
        assertThat(sagas).isNotEmpty();
        return sagas.stream()
                .max(Comparator.comparing(OrderSagaJpaEntity::getId))
                .orElseThrow();
    }

    private void assertOrderSaga(
            OrderSagaJpaEntity sagaEntity,
            String orderId,
            String sagaId,
            OrderSagaStatus expectedSagaStatus
    ) {
        assertThat(orderId).isNotBlank();
        assertThat(sagaId).isNotBlank();
        assertThat(sagaEntity.getOrderId()).isEqualTo(orderId);
        assertThat(sagaEntity.getSagaId()).isEqualTo(sagaId);
        assertThat(sagaEntity.getStatus()).isEqualTo(expectedSagaStatus);
        assertThat(sagaEntity.getItems()).hasSize(2);
    }

    private void assertOutbox(
            String orderId,
            MSAStatus expectedCouponStatus,
            MSAStatus expectedPointStatus,
            OrderSagaStatus expectedSagaStatus,
            boolean expectPayload
    ) {
        Optional<OutboxMessageJpaEntity> outboxOpt = outboxMessageJpaRepository.findByOrderId(orderId);
        assertThat(outboxOpt).isPresent();

        OutboxMessageJpaEntity outboxEntity = outboxOpt.get();
        assertThat(outboxEntity.getOrderId()).isEqualTo(orderId);
        assertThat(outboxEntity.getCouponStatus()).isEqualTo(expectedCouponStatus);
        assertThat(outboxEntity.getPointStatus()).isEqualTo(expectedPointStatus);
        assertThat(outboxEntity.getOrderStatus()).isEqualTo(MSAStatus.InProgress);
        assertThat(outboxEntity.getSagaStatus()).isEqualTo(expectedSagaStatus);
        if (expectPayload) {
//--- 생략 ...
}
```
- 설명: Kafka 이벤트를 발행해 서비스 간 비동기 연계를 구성한다.

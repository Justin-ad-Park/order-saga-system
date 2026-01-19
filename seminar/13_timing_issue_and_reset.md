# 13. 타이밍 이슈와 데이터 리셋

## 목표
- 보상 타이밍 이슈를 발견하고 해결하는 과정을 이해한다.

## 스토리라인
- 이벤트 순서/지연이 꼬이면서 보상 타이밍 이슈가 발생.
- 리셋/스냅샷 도구로 반복 검증 환경을 만들고 해결.

## 관련 커밋
- `c4401c7`, `a16fa0c`, `bfa985f`, `1864862`, `eeca8aa`, `c1100d4`

## 커밋 변경 요약
| 커밋 | 주요 변경 요약 | 체크아웃 |
| --- | --- | --- |
| `c4401c7` | Istio 설치. 강제 타임아웃 테스트용 로직 추가, *** 보상 트랜잭션 타이밍 오류 존재함 *** | `git checkout c4401c7` |
| `a16fa0c` | Timing Issue를 잡기 위한 로직 추가 | `git checkout a16fa0c` |
| `bfa985f` | 타이밍 이슈 작업 완료 | `git checkout bfa985f` |
| `1864862` | timing issue 테스트 케이스 추가 | `git checkout 1864862` |
| `eeca8aa` | Saga 반복 테스트 가능하도록 데이터 초기화(OUTBOX_MESSAGE, ORDER_SAGA, ORDER_ITEM) | `git checkout eeca8aa` |
| `c1100d4` | Snapshot 생성 시점을 sh에서 각 서비스 기동 스크립트(*schema.sql)로 변경 | `git checkout c1100d4` |

## 핵심 개념
- 보상 타이밍 이슈 원인 분석
- 스냅샷 기반 리셋 절차

## 기술/기능/프로세스
- 기술: 스냅샷 프로시저, 리셋 스크립트
- 기능: 보상 타이밍 이슈 재현/해결
- MSA: 상태 일관성 유지 전략
- EDA: 이벤트 순서/지연 영향 분석
## 데모/실습
- 리셋: `bin_common/05_reset_test_data.sh`
- 보상 테스트: `bin_istio_test/05_test_saga_compensation.sh`

## 커밋 상세
### c4401c7 Istio 설치. 강제 타임아웃 테스트용 로직 추가, *** 보상 트랜잭션 타이밍 오류 존재함 ***
- 주요 변경: Istio 설치. 강제 타임아웃 테스트용 로직 추가, *** 보상 트랜잭션 타이밍 오류 존재함 ***
- 핵심 코드: `point-service/src/main/java/com/example/pointservice/application/service/ReservePointService.java`
```java
public class ReservePointService implements ReservePointUseCase, ConfirmPointUseCase, CompensatePointUseCase {
//--- 생략 ...
    public void reserve(String pointNumber, String orderId) {
        maybeDelay(pointNumber);
        updateStatus(pointNumber, PointStatus.RESERVED, this::validateReservable);
    }
//--- 생략 ...
}
```
- 설명: 오케스트레이터가 쿠폰/포인트 예약을 병렬 호출해 분산 트랜잭션의 시작점을 만든다.

### a16fa0c Timing Issue를 잡기 위한 로직 추가
- 주요 변경: Timing Issue를 잡기 위한 로직 추가
- 핵심 코드: `point-service/src/test/java/com/example/pointservice/application/service/ReservePointServiceTest.java`
```java
class ReservePointServiceTest {
//--- 생략 ...
    void reserve_shouldChangeStatusToReserved_andSave() {
        // given
        String pointNumber = "PNT-UNIT-AVAILABLE-001";
        LocalDateTime now = LocalDateTime.now();
        Point availablePoint = new Point(pointNumber, PointStatus.AVAILABLE, now.minusDays(1), now.plusDays(1));

        when(loadPointPort.loadPoint(pointNumber)).thenReturn(Optional.of(availablePoint));

        // when
        reservePointService.reserve(pointNumber, "ORD-001");

        // then
        verify(loadPointPort, times(1)).loadPoint(pointNumber);
        verify(savePointPort, times(1)).save(argThat(saved ->
                saved.pointNumber().equals(pointNumber)
                        && saved.status() == PointStatus.RESERVED
        ));
    }
//--- 생략 ...
}
```
- 설명: 오케스트레이터가 쿠폰/포인트 예약을 병렬 호출해 분산 트랜잭션의 시작점을 만든다.

### bfa985f 타이밍 이슈 작업 완료
- 주요 변경: 타이밍 이슈 작업 완료
- 핵심 코드: `order-orchestrator/src/main/java/com/example/orderorchestrator/application/service/ReserveExternalResourcesService.java`
```java
public class ReserveExternalResourcesService {
//--- 생략 ...
    public Mono<Void> reserveExternalResources(String orderId, String couponNumber, String pointNumber) {
        List<Mono<?>> calls = new ArrayList<>();
        // Reserve independently; failures are collected and surfaced after all attempts.
        if (StringUtils.hasText(couponNumber)) {
            calls.add(reserveCoupon(couponNumber, orderId));
        }
        if (StringUtils.hasText(pointNumber)) {
            calls.add(reservePoint(pointNumber, orderId));
        }
        if (calls.isEmpty()) {
            return Mono.empty();
        }
        return Mono.whenDelayError(calls).then();
    }
//--- 생략 ...
}
```
- 설명: 오케스트레이터가 쿠폰/포인트 예약을 병렬 호출해 분산 트랜잭션의 시작점을 만든다.

### 1864862 timing issue 테스트 케이스 추가
- 주요 변경: timing issue 테스트 케이스 추가
- 핵심 코드: `bin_k8s/sql/create_test_snapshots.sql`
```sql
//--- 생략 ...
INSERT INTO coupon_snapshot
SELECT *
FROM coupon;


DROP PROCEDURE IF EXISTS sp_reset_coupon_test_data;
DELIMITER $$
CREATE PROCEDURE sp_reset_coupon_test_data()
BEGIN
//--- 생략 ...
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.

### eeca8aa Saga 반복 테스트 가능하도록 데이터 초기화(OUTBOX_MESSAGE, ORDER_SAGA, ORDER_ITEM)
- 주요 변경: Saga 반복 테스트 가능하도록 데이터 초기화(OUTBOX_MESSAGE, ORDER_SAGA, ORDER_ITEM)
- 핵심 코드: `bin_k8s/sql/create_test_snapshots.sql`
```sql
//--- 생략 ...
END$$
DELIMITER ;

CREATE DATABASE IF NOT EXISTS order_orchestrator_db;
USE order_orchestrator_db;

DROP PROCEDURE IF EXISTS sp_truncate_order_orchestrator_test_data;
DELIMITER $$
CREATE PROCEDURE sp_truncate_order_orchestrator_test_data()
//--- 생략 ...
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.

### c1100d4 Snapshot 생성 시점을 sh에서 각 서비스 기동 스크립트(*schema.sql)로 변경
- 주요 변경: Snapshot 생성 시점을 sh에서 각 서비스 기동 스크립트(*schema.sql)로 변경
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
        assertOrderSaga(
                sagaEntity,
                orderId,
                sagaId,
                readString(requestBody, "pointNumber"),
                expectedSagaStatus
        );
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

        assertOrderSaga(
                sagaEntity,
                orderId,
                sagaEntity.getSagaId(),
                readString(requestBody, "pointNumber"),
                expectedSagaStatus
        );
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
            String expectedPointNumber,
            OrderSagaStatus expectedSagaStatus
    ) {
        assertThat(orderId).isNotBlank();
        assertThat(sagaId).isNotBlank();
        assertThat(sagaEntity.getOrderId()).isEqualTo(orderId);
        assertThat(sagaEntity.getSagaId()).isEqualTo(sagaId);
        assertThat(sagaEntity.getPointNumber()).isEqualTo(expectedPointNumber);
        assertThat(sagaEntity.getStatus()).isEqualTo(expectedSagaStatus);
        assertThat(sagaEntity.getItems()).hasSize(2);
    }

    private String readString(Map<String, Object> requestBody, String key) {
        Object value = requestBody.get(key);
        return value == null ? null : value.toString();
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

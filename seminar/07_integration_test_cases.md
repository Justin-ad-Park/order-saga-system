# 07. 통합 테스트 시나리오 확장

## 목표
- 쿠폰/포인트 조합별 통합 테스트를 설계할 수 있다.

## 스토리라인
- 실패 케이스가 등장하면서, 테스트 데이터와 시나리오를 체계화.

## 관련 커밋
- `b058e04`, `fc9cbda`, `177839d`

## 커밋 변경 요약
| 커밋 | 주요 변경 요약 | 체크아웃 |
| --- | --- | --- |
| `b058e04` | Test Case 추가. 쿠폰만, 포인트만, 둘 다, 하나도 없음 | `git checkout b058e04` |
| `fc9cbda` | - 통합 테스트 리팩토링 - 쿠폰 실패, 포인트 성공 케이스 추가 | `git checkout fc9cbda` |
| `177839d` | - 로컬 테스트 시 order-orchestrator의 기존 테스트 데이터 삭제되도록 추가 | `git checkout 177839d` |

## 핵심 개념
- 케이스 분리: 쿠폰만/포인트만/둘 다/없음
- 반복 테스트 가능한 데이터 초기화

## 기술/기능/프로세스
- 기술: JUnit, SpringBootTest, TestRestTemplate
- 기능: 조합별 시나리오 검증, 데이터 초기화
- MSA: 쿠폰/포인트 조합별 테스트
- EDA: 실패/보상 케이스를 이벤트 이전 단계에서 검증
## 데모/실습
- 통합 테스트 확인: `order-orchestrator/src/test/java/.../OrderOrchestrationIntegrationTest.java`
- 리셋 스크립트: `bin_common/05_reset_test_data.sh`

## 커밋 상세
### b058e04 Test Case 추가. 쿠폰만, 포인트만, 둘 다, 하나도 없음
- 주요 변경: Test Case 추가. 쿠폰만, 포인트만, 둘 다, 하나도 없음
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

    @AfterEach
    void tearDown() {
        outboxMessageJpaRepository.deleteAll();
        orderSagaJpaRepository.deleteAll();
    }

    @Test
    void createOrder_withCouponAndPoint_shouldPersistOrderSaga_and_OutboxMessage() {
        // given: 주문 생성 요청 바디
        Map<String, Object> requestBody = Map.of(
                "couponNumber", "CPN-BOTH-001",
                "pointNumber", "PNT-BOTH-001",
                "paymentNumber", "PAY-001",
                "paymentAmount", 35000L,
                "orderItems", List.of(
                        Map.of("itemNumber", "ITEM-001", "quantity", 2),
                        Map.of("itemNumber", "ITEM-002", "quantity", 1)
                )
        );

        assertOrderCreated(requestBody);
    }

    @Test
    void createOrder_withCouponOnly_shouldPersistOrderSaga_and_OutboxMessage() {
        Map<String, Object> requestBody = Map.of(
                "couponNumber", "CPN-ONLY-001",
                "paymentNumber", "PAY-001",
                "paymentAmount", 35000L,
                "orderItems", List.of(
                        Map.of("itemNumber", "ITEM-001", "quantity", 2),
                        Map.of("itemNumber", "ITEM-002", "quantity", 1)
                )
        );

        assertOrderCreated(requestBody);
    }

    @Test
    void createOrder_withPointOnly_shouldPersistOrderSaga_and_OutboxMessage() {
        Map<String, Object> requestBody = Map.of(
                "pointNumber", "PNT-ONLY-001",
                "paymentNumber", "PAY-001",
                "paymentAmount", 35000L,
                "orderItems", List.of(
                        Map.of("itemNumber", "ITEM-001", "quantity", 2),
                        Map.of("itemNumber", "ITEM-002", "quantity", 1)
                )
        );

        assertOrderCreated(requestBody);
    }

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

        assertOrderCreated(requestBody);
    }

    private void assertOrderCreated(Map<String, Object> requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);

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
        assertThat(sagaEntity.getOrderId()).isEqualTo(orderId);
        assertThat(sagaEntity.getSagaId()).isEqualTo(sagaId);
        assertThat(sagaEntity.getStatus()).isEqualTo(OrderSagaStatus.InProgress);
        assertThat(sagaEntity.getItems()).hasSize(2);

        // 2) outbox_message 테이블
        Optional<OutboxMessageJpaEntity> outboxOpt = outboxMessageJpaRepository.findByOrderId(orderId);
        assertThat(outboxOpt).isPresent();

        OutboxMessageJpaEntity outboxEntity = outboxOpt.get();
        assertThat(outboxEntity.getOrderId()).isEqualTo(orderId);
        assertThat(outboxEntity.getCouponStatus()).isEqualTo(MSAStatus.InProgress);
        assertThat(outboxEntity.getPointStatus()).isEqualTo(MSAStatus.InProgress);
        assertThat(outboxEntity.getOrderStatus()).isEqualTo(MSAStatus.InProgress);
        assertThat(outboxEntity.getPaymentStatus()).isEqualTo(MSAStatus.InProgress);
        assertThat(outboxEntity.getSagaStatus()).isEqualTo(OrderSagaStatus.InProgress);
        assertThat(outboxEntity.getPayload()).isEqualTo("{}");
    }

    private static ServiceContext startService(
            Class<?> applicationClass,
            String configName,
            String schemaFileName,
            int fallbackPort,
            String serviceName
    ) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(applicationClass)
                .properties(
                        "server.port=0",
                        "spring.profiles.active=test",
                        "spring.config.name=" + configName
                )
                .run();

        int port;
        if (context instanceof ServletWebServerApplicationContext servletContext) {
//--- 생략 ...
}
```
- 설명: Outbox에 이벤트를 적재해 DB 트랜잭션과 이벤트 발행을 분리한다.

### fc9cbda - 통합 테스트 리팩토링 - 쿠폰 실패, 포인트 성공 케이스 추가
- 주요 변경: - 통합 테스트 리팩토링 - 쿠폰 실패, 포인트 성공 케이스 추가
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
                "couponNumber", "CPN-BOTH-001",
                "pointNumber", "PNT-BOTH-001",
                "paymentNumber", "PAY-001",
                "paymentAmount", 35000L,
                "orderItems", List.of(
                        Map.of("itemNumber", "ITEM-001", "quantity", 2),
                        Map.of("itemNumber", "ITEM-002", "quantity", 1)
                )
        );

        assertOrderCreated(requestBody, MSAStatus.Reserved, MSAStatus.Reserved);
    }

    // 쿠폰만 사용하는 경우
    @Test
    void createOrder_withCouponOnly_shouldPersistOrderSaga_and_OutboxMessage() {
        Map<String, Object> requestBody = Map.of(
                "couponNumber", "CPN-ONLY-001",
                "paymentNumber", "PAY-001",
                "paymentAmount", 35000L,
                "orderItems", List.of(
                        Map.of("itemNumber", "ITEM-001", "quantity", 2),
                        Map.of("itemNumber", "ITEM-002", "quantity", 1)
                )
        );

        assertOrderCreated(requestBody, MSAStatus.Reserved, MSAStatus.NotUsed);
    }

    // 포인트만 사용하는 경우
    @Test
    void createOrder_withPointOnly_shouldPersistOrderSaga_and_OutboxMessage() {
        Map<String, Object> requestBody = Map.of(
                "pointNumber", "PNT-ONLY-001",
                "paymentNumber", "PAY-001",
                "paymentAmount", 35000L,
                "orderItems", List.of(
                        Map.of("itemNumber", "ITEM-001", "quantity", 2),
                        Map.of("itemNumber", "ITEM-002", "quantity", 1)
                )
        );

        assertOrderCreated(requestBody, MSAStatus.NotUsed, MSAStatus.Reserved);
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

        assertOrderCreated(requestBody, MSAStatus.NotUsed, MSAStatus.NotUsed);
    }

    // 쿠폰은 이미 예약되어 실패하고, 포인트는 예약 가능한 경우
    @Test
    void createOrder_withReservedCouponAndAvailablePoint_shouldMarkCouponFailedAndPointReserved() {
        Map<String, Object> requestBody = Map.of(
                "couponNumber", "CPN-BOTH-RESERVED-001",
                "pointNumber", "PNT-BOTH-AVAILABLE-001",
                "paymentNumber", "PAY-001",
                "paymentAmount", 35000L,
                "orderItems", List.of(
                        Map.of("itemNumber", "ITEM-001", "quantity", 2),
                        Map.of("itemNumber", "ITEM-002", "quantity", 1)
                )
        );

        assertOrderCreatedWithExternalFailure(requestBody, MSAStatus.Failed, MSAStatus.Reserved);
    }

    private void assertOrderCreated(Map<String, Object> requestBody, MSAStatus expectedCouponStatus, MSAStatus expectedPointStatus) {
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
        assertOrderSaga(sagaEntity, orderId, sagaId);
        assertOutbox(orderId, expectedCouponStatus, expectedPointStatus, true);
    }

    private void assertOrderCreatedWithExternalFailure(Map<String, Object> requestBody, MSAStatus expectedCouponStatus, MSAStatus expectedPointStatus) {
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

        assertOrderSaga(sagaEntity, orderId, sagaEntity.getSagaId());
        assertOutbox(orderId, expectedCouponStatus, expectedPointStatus, false);
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

    private void assertOrderSaga(OrderSagaJpaEntity sagaEntity, String orderId, String sagaId) {
        assertThat(orderId).isNotBlank();
        assertThat(sagaId).isNotBlank();
        assertThat(sagaEntity.getOrderId()).isEqualTo(orderId);
        assertThat(sagaEntity.getSagaId()).isEqualTo(sagaId);
        assertThat(sagaEntity.getStatus()).isEqualTo(OrderSagaStatus.InProgress);
        assertThat(sagaEntity.getItems()).hasSize(2);
    }

    private void assertOutbox(
            String orderId,
            MSAStatus expectedCouponStatus,
            MSAStatus expectedPointStatus,
            boolean expectPayload
    ) {
        Optional<OutboxMessageJpaEntity> outboxOpt = outboxMessageJpaRepository.findByOrderId(orderId);
        assertThat(outboxOpt).isPresent();

        OutboxMessageJpaEntity outboxEntity = outboxOpt.get();
        assertThat(outboxEntity.getOrderId()).isEqualTo(orderId);
        assertThat(outboxEntity.getCouponStatus()).isEqualTo(expectedCouponStatus);
        assertThat(outboxEntity.getPointStatus()).isEqualTo(expectedPointStatus);
        assertThat(outboxEntity.getOrderStatus()).isEqualTo(MSAStatus.InProgress);
        assertThat(outboxEntity.getPaymentStatus()).isEqualTo(MSAStatus.InProgress);
        assertThat(outboxEntity.getSagaStatus()).isEqualTo(OrderSagaStatus.InProgress);
        if (expectPayload) {
//--- 생략 ...
}
```
- 설명: Outbox에 이벤트를 적재해 DB 트랜잭션과 이벤트 발행을 분리한다.

### 177839d - 로컬 테스트 시 order-orchestrator의 기존 테스트 데이터 삭제되도록 추가
- 주요 변경: - 로컬 테스트 시 order-orchestrator의 기존 테스트 데이터 삭제되도록 추가
- 핵심 코드: `scripts/stop_local_msa.sh`
```bash
//--- 생략 ...
```
- 설명: 실행/테스트 스크립트를 통해 분산 시나리오를 재현한다.

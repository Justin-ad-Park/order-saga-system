# 05. 포인트 서비스 구축과 MSA 확장

## 목표
- 포인트 서비스 추가로 MSA 구성이 확장되는 과정을 이해한다.

## 스토리라인
- 쿠폰 서비스 패턴을 포인트로 확장하면서 중복과 재사용 포인트를 찾음.

## 관련 커밋
- `193e5e2`, `6bb3683`, `34f3209`

## 커밋 변경 요약
| 커밋 | 주요 변경 요약 | 체크아웃 |
| --- | --- | --- |
| `193e5e2` | First commit for Point MSA | `git checkout 193e5e2` |
| `6bb3683` | point-service, coupon-service 테스트 케이스 추가 | `git checkout 6bb3683` |
| `34f3209` | 통합 테스트 확장 | `git checkout 34f3209` |

## 핵심 개념
- 비슷한 서비스 간 계약 일관성
- 테스트 케이스 확장

## 기술/기능/프로세스
- 기술: Spring Boot, JPA, MySQL, REST
- 기능: 포인트 reserve/confirm/compensate
- MSA: 포인트 서비스 독립 배포
- EDA: 쿠폰과 동일한 계약으로 이벤트 흐름에 참여
## 데모/실습
- 테스트 데이터 확인: `point-service/src/main/resources/point_schema.sql`
- 통합 테스트: `point-service/src/test/java/.../PointControllerIntegrationTest.java`

## 커밋 상세
### 193e5e2 First commit for Point MSA
- 주요 변경: First commit for Point MSA
- 핵심 코드: `point-service/src/test/java/com/example/pointservice/application/service/ReservePointServiceTest.java`
```java
class ReservePointServiceTest {
//--- 생략 ...
    void reserve_shouldChangeStatusToReserved_andSave() {
        // given
        String pointNumber = "PNT-001";
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

### 6bb3683 point-service, coupon-service 테스트 케이스 추가
- 주요 변경: point-service, coupon-service 테스트 케이스 추가
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

### 34f3209 통합 테스트 확장
- 주요 변경: 통합 테스트 확장
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

    private void assertOrderCreated(Map<String, Object> requestBody, MSAStatus expectedCouponStatus, MSAStatus expectedPointStatus) {
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
        assertThat(outboxEntity.getCouponStatus()).isEqualTo(expectedCouponStatus);
        assertThat(outboxEntity.getPointStatus()).isEqualTo(expectedPointStatus);
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

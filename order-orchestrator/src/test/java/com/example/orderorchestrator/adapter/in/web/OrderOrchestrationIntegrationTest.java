// src/test/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationIntegrationTest.java
package com.example.orderorchestrator.adapter.in.web;

import com.example.orderorchestrator.adapter.in.web.dto.response.CreateOrderResponse;
import com.example.couponservice.CouponServiceApplication;
import com.example.pointservice.PointServiceApplication;
import com.example.orderorchestrator.adapter.out.persistence.jpa.OrderSagaJpaRepository;
import com.example.orderorchestrator.adapter.out.persistence.jpa.OutboxMessageJpaRepository;
import com.example.orderorchestrator.adapter.out.persistence.jpa.entity.OrderSagaJpaEntity;
import com.example.orderorchestrator.adapter.out.persistence.jpa.entity.OutboxMessageJpaEntity;
import com.example.orderorchestrator.domain.model.status.MSAStatus;
import com.example.orderorchestrator.domain.model.status.OrderSagaStatus;
import com.example.orderorchestrator.adapter.out.kafka.KafkaTopicPrinter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *  CLI Test 방법
 *  ./gradlew :order-orchestrator:test --tests "OrderOrchestrationIntegrationTest"
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.name=orderOS_application")
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Sql(
        scripts = "/orderOS_cleanup.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
)
@Transactional(isolation = Isolation.READ_COMMITTED)
class OrderOrchestrationIntegrationTest {

    private static ConfigurableApplicationContext couponContext;
    private static int couponPort;
    private static ConfigurableApplicationContext pointContext;
    private static int pointPort;

    @AfterAll
    void stopMSAService() {
        KafkaTopicPrinter.printKafkaTopics(bootstrapServers, null);
        if (couponContext != null) {
            couponContext.close();
        }
        if (pointContext != null) {
            pointContext.close();
        }
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        if (couponContext == null) {
            ServiceContext context = startService(
                    CouponServiceApplication.class,
                    "coupon_application",
                    "coupon_schema.sql",
                    8081,
                    "coupon"
            );
            couponContext = context.context();
            couponPort = context.port();
        }

        registry.add("external.coupon.base-url", () -> "http://localhost:" + couponPort);

        if (pointContext == null) {
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
            assertThat(outboxEntity.getPayload()).isEqualTo("{}");
        }
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
            port = servletContext.getWebServer().getPort();
        } else {
            port = context.getEnvironment().getProperty("local.server.port", Integer.class, fallbackPort);
        }

        System.out.println("\n==========================");
        System.out.println(serviceName.toUpperCase() + "_PORT: " + port);
        System.out.println(serviceName + " spring.datasource.url = " +
                context.getEnvironment().getProperty("spring.datasource.url"));

        System.out.println(serviceName + " spring.sql.init.mode = " +
                context.getEnvironment().getProperty("spring.sql.init.mode"));

        System.out.println(serviceName + " spring.sql.init.schema-locations = " +
                context.getEnvironment().getProperty("spring.sql.init.schema-locations"));

        var schemaResource = context.getResource("classpath:/" + schemaFileName);
        System.out.println(schemaFileName + " exists? " + schemaResource.exists() + ", url=" + schemaResource);
        System.out.println("==========================");

        return new ServiceContext(context, port);
    }

    private static final class ServiceContext {
        private final ConfigurableApplicationContext context;
        private final int port;

        private ServiceContext(ConfigurableApplicationContext context, int port) {
            this.context = context;
            this.port = port;
        }

        private ConfigurableApplicationContext context() {
            return context;
        }

        private int port() {
            return port;
        }
    }

}

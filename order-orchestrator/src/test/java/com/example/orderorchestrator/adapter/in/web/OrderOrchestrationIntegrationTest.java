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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
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
import org.springframework.transaction.annotation.Transactional;

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
@Transactional
@Sql(
        scripts = "/orderOS_cleanup.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
)
class OrderOrchestrationIntegrationTest {

    private static ConfigurableApplicationContext couponContext;
    private static int couponPort;
    private static ConfigurableApplicationContext pointContext;
    private static int pointPort;

    @AfterAll
    static void stopMSAService() {
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

    @AfterEach
    void tearDown() {
        outboxMessageJpaRepository.deleteAll();
        orderSagaJpaRepository.deleteAll();
    }

    @Test
    void createOrder_shouldPersistOrderSaga_and_OutboxMessage() {
        // given: 주문 생성 요청 바디
        Map<String, Object> requestBody = Map.of(
                "couponNumber", "CPN-001",
                "pointNumber", "PNT-001",
                "paymentNumber", "PAY-001",
                "paymentAmount", 35000L,
                "orderItems", List.of(
                        Map.of("itemNumber", "ITEM-001", "quantity", 2),
                        Map.of("itemNumber", "ITEM-002", "quantity", 1)
                )
        );

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

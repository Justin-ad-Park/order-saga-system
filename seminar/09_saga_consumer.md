# 09. Saga Consumer 구현과 보상 호출

## 목표
- 소비자에서 이벤트를 처리하고 confirm/compensate를 수행하는 흐름을 이해한다.

## 스토리라인
- 오케스트레이터 이벤트를 소비하여 실제 MSA 상태를 확정/보상.

## 관련 커밋
- `3afbfb9`, `0b73be2`, `a1f74d8`, `576a868`, `5a250f8`, `9e08ba1`

## 커밋 변경 요약
| 커밋 | 주요 변경 요약 | 체크아웃 |
| --- | --- | --- |
| `3afbfb9` | Comsumer 기본 프로젝트 및 기본 로직 구성 | `git checkout 3afbfb9` |
| `0b73be2` | ### Saga 컨슈머 confirm, compensate 로직 추가 ### | `git checkout 0b73be2` |
| `a1f74d8` | ### Consumer host Test ### | `git checkout a1f74d8` |
| `576a868` | Comsumer 실행 시 profile 설정 안되는 오류 수정 | `git checkout 576a868` |
| `5a250f8` | ### Saga Local & K8s + Host Consumer 테스트 완료 ### | `git checkout 5a250f8` |
| `9e08ba1` | ### Consumer K8s 배포 및 실행 스크립트 추사 ### | `git checkout 9e08ba1` |

## 핵심 개념
- 소비자 책임(메시지 처리, 상태 갱신)
- 로컬/호스트/K8s 실행 분리

## 기술/기능/프로세스
- 기술: Spring Kafka Consumer, WebClient
- 기능: 이벤트 처리, confirm/compensate 호출
- MSA: 소비자 역할 분리
- EDA: order-saga-events 소비
## 데모/실습
- 소비자 실행: `bin_k8s/07_run_local_consumer.sh`, `bin_k8s/07_run_consumer_host2K8s.sh`

## 커밋 상세
### 3afbfb9 Comsumer 기본 프로젝트 및 기본 로직 구성
- 주요 변경: Comsumer 기본 프로젝트 및 기본 로직 구성
- 핵심 코드: `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/adapter/in/kafka/OrderSagaEventConsumer.java`
```java
package com.example.ordersagaconsumer.adapter.in.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import com.example.ordersagaconsumer.application.port.in.ProcessOrderSagaEventUseCase;
import com.example.ordersagaconsumer.adapter.in.kafka.dto.OrderSagaEventPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderSagaEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessOrderSagaEventUseCase processOrderSagaEventUseCase;

    public OrderSagaEventConsumer(
            ObjectMapper objectMapper,
            ProcessOrderSagaEventUseCase processOrderSagaEventUseCase
    ) {
        this.objectMapper = objectMapper;
        this.processOrderSagaEventUseCase = processOrderSagaEventUseCase;
    }

    @KafkaListener(
            topics = "${order.saga.events.topic}",
            groupId = "${order.saga.events.consumer-group:order-saga-consumer}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        OrderSagaEventPayload payload = readPayload(record.value());
        if (payload == null) {
            return;
        }
        processOrderSagaEventUseCase.process(payload.orderId(), payload.status());
    }

    private OrderSagaEventPayload readPayload(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, OrderSagaEventPayload.class);
        } catch (Exception ex) {
            System.out.println("### Kafka payload parse failed ### : message=" + ex.getMessage()
                    + " payload=" + rawPayload);
            return null;
        }
    }
}

```
- 설명: Kafka 컨슈머가 이벤트를 수신해 confirm/compensate 흐름을 이어간다.

### 0b73be2 ### Saga 컨슈머 confirm, compensate 로직 추가 ###
- 주요 변경: ### Saga 컨슈머 confirm, compensate 로직 추가 ###

## 이벤트 처리 서비스
`order-saga-consumer/src/main/java/com/example/ordersagaconsumer/application/service/ProcessOrderSagaEventService.java`

```java
package com.example.ordersagaconsumer.application.service;

import com.example.ordersagaconsumer.application.port.in.ProcessOrderSagaEventUseCase;
import com.example.ordersagaconsumer.application.port.out.CouponServicePort;
import com.example.ordersagaconsumer.application.port.out.LoadOrderSagaPort;
import com.example.ordersagaconsumer.application.port.out.PointServicePort;
import com.example.ordersagaconsumer.application.port.out.UpdateOutboxMessagePort;
import com.example.ordersagaconsumer.domain.model.OrderSagaInfo;
import com.example.common.status.MSAStatus;
import com.example.common.status.OrderSagaStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProcessOrderSagaEventService implements ProcessOrderSagaEventUseCase {

    private final LoadOrderSagaPort loadOrderSagaPort;
    private final CouponServicePort couponServicePort;
    private final PointServicePort pointServicePort;
    private final UpdateOutboxMessagePort updateOutboxMessagePort;
    private final SagaStatusTransitionService sagaStatusTransitionService;

    public ProcessOrderSagaEventService(
            LoadOrderSagaPort loadOrderSagaPort,
            CouponServicePort couponServicePort,
            PointServicePort pointServicePort,
            UpdateOutboxMessagePort updateOutboxMessagePort,
            SagaStatusTransitionService sagaStatusTransitionService
    ) {
        this.loadOrderSagaPort = loadOrderSagaPort;
        this.couponServicePort = couponServicePort;
        this.pointServicePort = pointServicePort;
        this.updateOutboxMessagePort = updateOutboxMessagePort;
        this.sagaStatusTransitionService = sagaStatusTransitionService;
    }

    @Override
    public void process(String orderId, String status) {
        if (orderId == null || orderId.isBlank()) {
            System.out.println("### OrderSaga lookup skipped ### : empty orderId");
            return;
        }

        OrderSagaInfo info = loadOrderSagaPort.findByOrderId(orderId)
                .orElse(null);

        if (info == null) {
            System.out.println("### OrderSaga not found ### : orderId=" + orderId
                    + " status=" + status);
            return;
        }

        System.out.println("### OrderSaga details ### : orderId=" + orderId
                + " status=" + status
                + " couponNumber=" + info.couponNumber()
                + " pointNumber=" + info.pointNumber());

        OrderSagaStatus sagaStatus = parseSagaStatus(status);
        if (sagaStatus == null) {
            System.out.println("### OrderSaga status skipped ### : unsupported status=" + status);
            return;
        }

        if (sagaStatus == OrderSagaStatus.Reserved) {
            handleConfirm(orderId, info);
            return;
        }

        if (sagaStatus == OrderSagaStatus.Compensating) {
            handleCompensate(orderId, info);
        }
    }

    private void handleConfirm(String orderId, OrderSagaInfo info) {
        boolean couponNeeded = StringUtils.hasText(info.couponNumber());
        boolean pointNeeded = StringUtils.hasText(info.pointNumber());

        boolean couponOk = true;
        boolean pointOk = true;

        if (couponNeeded) {
            couponOk = couponServicePort.confirm(info.couponNumber(), orderId);
            updateOutboxMessagePort.updateCouponStatus(
                    orderId,
                    couponOk ? MSAStatus.Completed : MSAStatus.Failed
            );
        }

        if (pointNeeded) {
            pointOk = pointServicePort.confirm(info.pointNumber(), orderId);
            updateOutboxMessagePort.updatePointStatus(
                    orderId,
                    pointOk ? MSAStatus.Completed : MSAStatus.Failed
            );
        }

        if (couponOk && pointOk) {
            sagaStatusTransitionService.markCompleted(orderId);
        }
    }

    private void handleCompensate(String orderId, OrderSagaInfo info) {
        boolean couponNeeded = StringUtils.hasText(info.couponNumber());
        boolean pointNeeded = StringUtils.hasText(info.pointNumber());

        boolean couponOk = true;
        boolean pointOk = true;

        if (couponNeeded) {
            couponOk = couponServicePort.compensate(info.couponNumber(), orderId);
            updateOutboxMessagePort.updateCouponStatus(
                    orderId,
                    couponOk ? MSAStatus.Compensated : MSAStatus.Failed
            );
        }

        if (pointNeeded) {
            pointOk = pointServicePort.compensate(info.pointNumber(), orderId);
            updateOutboxMessagePort.updatePointStatus(
                    orderId,
                    pointOk ? MSAStatus.Compensated : MSAStatus.Failed
            );
        }

        if (couponOk && pointOk) {
            sagaStatusTransitionService.markCompensated(orderId);
        }
    }

    private OrderSagaStatus parseSagaStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return OrderSagaStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

```

## 목표
Kafka 이벤트 수신 이후 confirm/compensate 처리 흐름을 이해한다.

## 쿠폰/포인트 confirm/compensate 호출
`order-saga-consumer/src/main/java/com/example/ordersagaconsumer/adapter/out/webclient/CouponServiceClient.java`
```java
package com.example.ordersagaconsumer.adapter.out.webclient;

import com.example.ordersagaconsumer.adapter.out.webclient.dto.CompensateCouponRequest;
import com.example.ordersagaconsumer.adapter.out.webclient.dto.ConfirmCouponRequest;
import com.example.ordersagaconsumer.adapter.out.webclient.support.ServiceClientSupport;
import com.example.ordersagaconsumer.application.port.out.CouponServicePort;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class CouponServiceClient extends ServiceClientSupport implements CouponServicePort {

    public CouponServiceClient(
            WebClient.Builder builder,
            @Value("${external.coupon.base-url}") String baseUrl,
            @Value("${external.client.timeout-seconds:3}") long timeoutSeconds,
            @Value("${external.client.retry-count:0}") int retryCount
    ) {
        super(builder, baseUrl, "Coupon", Duration.ofSeconds(timeoutSeconds), retryCount);
    }

    @Override
    public boolean confirm(String couponNumber, String orderId) {
        ConfirmCouponRequest request = new ConfirmCouponRequest(couponNumber, orderId);
        return post("/api/v1/coupons/confirm", request);
    }

    @Override
    public boolean compensate(String couponNumber, String orderId) {
        CompensateCouponRequest request = new CompensateCouponRequest(couponNumber, orderId);
        return post("/api/v1/coupons/compensate", request);
    }
}

```



`order-saga-consumer/src/main/java/com/example/ordersagaconsumer/adapter/out/webclient/PointServiceClient.java`
```java
package com.example.ordersagaconsumer.adapter.out.webclient;

import com.example.ordersagaconsumer.adapter.out.webclient.dto.CompensatePointRequest;
import com.example.ordersagaconsumer.adapter.out.webclient.dto.ConfirmPointRequest;
import com.example.ordersagaconsumer.adapter.out.webclient.support.ServiceClientSupport;
import com.example.ordersagaconsumer.application.port.out.PointServicePort;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PointServiceClient extends ServiceClientSupport implements PointServicePort {

    public PointServiceClient(
            WebClient.Builder builder,
            @Value("${external.point.base-url}") String baseUrl,
            @Value("${external.client.timeout-seconds:3}") long timeoutSeconds,
            @Value("${external.client.retry-count:0}") int retryCount
    ) {
        super(builder, baseUrl, "Point", Duration.ofSeconds(timeoutSeconds), retryCount);
    }

    @Override
    public boolean confirm(String pointNumber, String orderId) {
        ConfirmPointRequest request = new ConfirmPointRequest(pointNumber, orderId);
        return post("/api/v1/points/confirm", request);
    }

    @Override
    public boolean compensate(String pointNumber, String orderId) {
        CompensatePointRequest request = new CompensatePointRequest(pointNumber, orderId);
        return post("/api/v1/points/compensate", request);
    }
}

```

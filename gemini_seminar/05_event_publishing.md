# Chapter 5: Kafka에 이벤트 발행(Publish)하기

## 1. 개요: Saga 이벤트의 전파

이전 챕터에서 우리는 `Apache Kafka`를 구축하여 서비스 간 느슨한 결합을 위한 백본을 마련했습니다. 이제 `order-orchestrator`에서 발생한 중요한 비즈니스 이벤트, 즉 Saga의 상태 변경 이벤트를 Kafka 토픽으로 발행(Publish)하는 로직을 구현합니다. 이 이벤트들은 다른 서비스(특히 `order-saga-consumer`)가 Saga의 진행 상황을 인지하고 필요한 후속 조치(confirm/compensate)를 수행할 수 있도록 하는 핵심적인 연결 고리 역할을 합니다.

### 핵심 학습 목표
*   `order-orchestrator`에서 Saga 이벤트를 Kafka로 발행하는 과정을 이해합니다.
*   `OrderSagaEventKafkaPublisher`가 이벤트를 직렬화하고 Kafka로 전송하는 방법을 학습합니다.
*   `OrderOrchestrationController`에서 외부 서비스 호출 후 Saga 상태를 업데이트하고 이벤트를 발행하는 로직을 파악합니다.
*   Outbox 테이블의 현재 활용 방식과 폴링 퍼블리셔의 필요성에 대해 다시 한번 생각합니다.

## 2. 이벤트 발행 메커니즘

`order-orchestrator`는 주문 생성 요청을 처리하고 `coupon-service`, `point-service`와 같은 외부 MSA에 리소스 예약을 요청합니다. 이 외부 호출 결과에 따라 Saga의 전체적인 상태가 변화합니다. 이 변화된 Saga 상태를 다른 서비스에 알리기 위해 `order-orchestrator`는 Kafka로 이벤트를 발행합니다.

**현재 이벤트 발행 흐름:**
1.  `order-orchestrator`의 `OrderOrchestrationController`가 `CreateOrderUseCase`를 통해 주문을 생성합니다.
2.  이후 `ReserveExternalResourcesService`를 통해 `coupon-service`와 `point-service`에 비동기적으로(Reactor `Mono.whenDelayError`) 예약 요청을 보냅니다.
3.  이 외부 서비스 호출이 모두 완료되거나 실패하면, `OrderOrchestrationController`는 Saga의 상태를 `Reserved` 또는 `Compensating`으로 업데이트하고, 이어서 `OrderSagaEventService`를 통해 Kafka로 `OrderSagaEvent`를 직접 발행합니다.

**`Outbox Pattern`과 이벤트 발행 (재확인):**
Chapter 3에서 언급했듯이, `outbox_message` 테이블은 현재 Saga 상태를 데이터베이스 트랜잭션과 함께 안정적으로 기록하는 **신뢰성 있는 기록 저장소** 역할을 합니다. 하지만 `outbox_message` 테이블에 저장된 메시지를 Kafka로 중계하는 **Polling Publisher** 로직은 현재 프로젝트에 명시적으로 구현되어 있지 않습니다.
대신, `OrderOrchestrationController`가 외부 서비스 호출 완료 후 Kafka로 이벤트를 **직접 발행**하는 방식을 사용합니다. 이는 데모 및 학습의 간결성을 위한 선택일 수 있으며, 실제 프로덕션 환경에서는 `Outbox Pattern`의 '메시지 중계' 부분을 별도로 구현하여 데이터 일관성을 더욱 강력하게 보장해야 합니다.

## 3. 이벤트 발행 관련 Git 이력

Kafka 이벤트 발행 로직의 추가와 관련된 주요 Git 커밋입니다.

| 커밋 ID | 날짜 | 주요 변경 요약 |
|---|---|---|
| `9bc1014` | 2026-01-05 | `bin_k8s` 스크립트 설명 추가 및 `OrderSagaEventKafkaPublisher` 구현 |
| `aeceecc` | 2026-01-04 | 테스트 토픽 분리 및 `OrderSagaEventPublishIntegrationTest` 추가 |
| `9aa633c` | 2026-01-04 | 통합 테스트 Kafka 토픽 로그 추가 |

**(실습 가이드: Git 커밋 확인)**
1.  `git checkout 9bc1014` 명령어로 해당 커밋 시점으로 이동하여 `OrderSagaEventKafkaPublisher.java` 파일이 추가된 것을 확인해 보세요.
2.  `git diff aeceecc~1 aeceecc` 명령어로 `OrderSagaEventPublishIntegrationTest`가 어떻게 이벤트 발행을 테스트하는지 확인할 수 있습니다.

## 4. 핵심 코드 스니펫: Saga 이벤트 발행

### 4.1. `OrderSagaEventKafkaPublisher` (Driven Adapter)

`OrderSagaEventPublisher` (Output Port) 인터페이스를 구현하며, `OrderSagaEvent` 객체를 JSON 문자열로 직렬화하여 Kafka 토픽으로 전송하는 역할을 합니다.

**`order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/kafka/OrderSagaEventKafkaPublisher.java`**
```java
// ... imports ...
@Component
public class OrderSagaEventKafkaPublisher implements OrderSagaEventPublisher { // Output Port 구현체
    private static final Logger log = LoggerFactory.getLogger(OrderSagaEventKafkaPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate; // Spring Kafka 제공 템플릿
    private final ObjectMapper objectMapper; // JSON 직렬화를 위한 ObjectMapper
    private final String topic; // 이벤트를 발행할 Kafka 토픽 이름 (application.yml 에서 설정)

    public OrderSagaEventKafkaPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${order.saga.events.topic:order-saga-events}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publish(OrderSagaEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event); // 이벤트를 JSON 문자열로 직렬화
            // KafkaTemplate을 사용하여 지정된 토픽으로 이벤트 전송
            // event.orderId()를 메시지 키로 사용하여 특정 파티션으로 이벤트가 발행되도록 할 수 있음
            kafkaTemplate.send(topic, event.orderId(), payload);
            log.info("OrderSagaEvent published to Kafka: topic={}, orderId={}, type={}, status={}",
                     topic, event.orderId(), event.type(), event.status());
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize OrderSagaEvent: orderId={}", event.orderId(), ex);
        }
    }
}
```
**설명:** `KafkaTemplate`은 Spring Kafka에서 제공하는 편리한 도구로, Kafka 브로커로 메시지를 쉽게 보낼 수 있게 합니다. `ObjectMapper`를 사용하여 `OrderSagaEvent` 객체를 JSON 포맷으로 변환하는데, 이는 컨슈머가 이벤트를 쉽게 파싱할 수 있도록 합니다.

### 4.2. `OrderOrchestrationController`에서 이벤트 발행 (Driving Adapter)

`OrderOrchestrationController`는 외부 MSA 호출이 완료된 후, Saga의 최종 상태에 따라 `OrderSagaEventService`를 통해 Kafka 이벤트를 발행합니다.

**`order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`**
```java
// ... imports ...
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderOrchestrationController {

    private final CreateOrderUseCase createOrderUseCase;
    private final ReserveExternalResourcesService reserveExternalResourcesService;
    private final UpdateOutboxMessageUseCase updateOutboxMessageUseCase;
    private final UpdateOrderSagaStatusUseCase updateOrderSagaStatusUseCase;
    private final OrderSagaEventService orderSagaEventService; // 이벤트 발행 서비스

    @PostMapping
    public Mono<ResponseEntity<CreateOrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        CreateOrderCommand command = mapToCommand(request);
        CreateOrderResult result = createOrderUseCase.createOrder(command);

        return reserveExternalResourcesService.reserveExternalResources(
                        result.orderId(),
                        request.couponNumber(),
                        request.pointNumber()
                )
                // 외부 리소스 예약 성공 시: Saga 상태를 Reserved로 업데이트하고 이벤트 발행
                .then(Mono.fromRunnable(() -> {
                    updateSagaStatus(result.orderId(), OrderSagaStatus.Reserved);
                    publishSagaEvent(result, OrderSagaStatus.Reserved, OrderSagaEventType.RESERVE_SUCCEEDED);
                }))
                // 외부 리소스 예약 실패 시: Saga 상태를 Compensating으로 업데이트하고 이벤트 발행 (보상 시작)
                .onErrorResume(ex -> {
                    updateSagaStatus(result.orderId(), OrderSagaStatus.Compensating);
                    publishSagaEvent(result, OrderSagaStatus.Compensating, OrderSagaEventType.RESERVE_FAILED);
                    return Mono.error(ex); // 실패를 다시 전파
                })
                .thenReturn(ResponseEntity.ok(mapToResponse(result)));
    }

    // ... (mapToCommand, mapToResponse 등 헬퍼 메서드 생략) ...

    // Saga 상태 업데이트 헬퍼 메서드
    private void updateSagaStatus(String orderId, OrderSagaStatus status) {
        updateOrderSagaStatusUseCase.updateStatus(orderId, status);
        updateOutboxMessageUseCase.updateSagaStatus(orderId, status);
    }

    // Saga 이벤트 발행 헬퍼 메서드
    private void publishSagaEvent(CreateOrderResult result, OrderSagaStatus status, OrderSagaEventType type) {
        orderSagaEventService.publish(result.orderId(), result.sagaId(), status, type);
    }
}
```
**설명:** `reserveExternalResourcesService.reserveExternalResources()`의 결과에 따라 `then()` 또는 `onErrorResume()` 블록에서 `publishSagaEvent()` 헬퍼 메서드를 호출하여 Kafka 이벤트를 발행합니다. 이 방식은 외부 서비스 호출 결과가 나온 직후 이벤트를 발행하므로, `Outbox Pattern`의 메시지 중계 부분을 생략한 간결한 구현입니다.

## 5. 실습 체크포인트

`order-orchestrator`를 통해 주문을 생성하고, Kafka 토픽에 이벤트가 발행되었는지 확인합니다.

1.  **필수 서비스 실행:**
    *   Chapter 4에서 배포한 Kafka 클러스터가 실행 중인지 확인합니다. (필요 시 `bin_k8s/06_deploy_kafka.sh` 실행)
    *   `coupon-service`와 `order-orchestrator`가 모두 실행 중인지 확인합니다. (Chapter 2의 실습 가이드 참조)
2.  **`order-orchestrator`를 통한 주문 생성:**
    *   `order-orchestrator/src/test/httprequest/01_orderOrchestratorTest.http` 파일에서 "주문 생성 요청 (Happy Path 예시)"를 사용하여 API를 호출합니다.
    *   **요청 바디 예시:**
        ```json
        {
          "couponNumber": "CPN-INT-AVAILABLE-001",
          "pointNumber": "PNT-INT-AVAILABLE-001",
          "paymentNumber": "PAY-001",
          "paymentAmount": 35000,
          "orderItems": [
            {
              "itemNumber": "ITEM-001",
              "quantity": 2
            }
          ]
        }
        ```
    *   요청이 성공적으로 처리되면 `order-orchestrator` 로그에서 `OrderSagaEvent published to Kafka...`와 같은 메시지를 확인할 수 있습니다.
3.  **Kafka 토픽에서 이벤트 확인:**
    *   새로운 터미널을 열고 다음 명령어를 실행하여 `order-saga-events` 토픽의 메시지를 소비합니다:
        ```bash
        kubectl -n msa exec deploy/kafka -- /bin/bash -lc "/opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic order-saga-events --from-beginning --max-messages 1"
        ```
    *   **예상 결과:** `order-orchestrator`가 발행한 `OrderSagaEvent` JSON 메시지가 출력되는 것을 확인할 수 있습니다. `status`가 `Reserved`로 표시될 것입니다.

---
이제 `order-orchestrator`에서 Saga 이벤트를 Kafka로 성공적으로 발행하는 방법을 배웠습니다. 다음 챕터에서는 발행된 이벤트를 수신하여 Saga의 후속 로직(confirm/compensate)을 수행하는 `order-saga-consumer` 서비스를 구현하는 방법을 알아봅니다.
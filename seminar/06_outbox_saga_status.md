# 06. Outbox와 Saga 상태 전이

## 목표
- outbox 메시지와 saga_status 상태 전이 규칙을 이해한다.

## 스토리라인
- 이벤트 전파가 안정적이지 않아 outbox를 도입.
- saga_status 전이가 의미 있게 정의되어야 테스트가 안정됨.

## 관련 커밋
- `d95cb17`, `0d2221b`, `982ec0a`, `0531530`

## 커밋 변경 요약
| 커밋 | 주요 변경 요약 | 체크아웃 |
| --- | --- | --- |
| `d95cb17` | outbox_message MSA 상태 저장 로직 추가 | `git checkout d95cb17` |
| `0d2221b` | outboxMessage에 pointStatus 컬럼 추가 | `git checkout 0d2221b` |
| `982ec0a` | saga_status가 결과에 맞게 Reserved 또는 Compensating으로 업데이트 되도록 로직 수정 | `git checkout 982ec0a` |
| `0531530` | updateSagaStatus 메서드 리팩터링 | `git checkout 0531530` |

## 핵심 개념
- outbox_message와 order_saga 테이블 역할
- 상태 전이(Reserved/Compensating/Completed)

## 기술/기능/프로세스
- 기술: JPA/JDBC, outbox 테이블
- 기능: saga_status 전이, outbox_message 업데이트
- MSA: 오케스트레이터가 상태 추적
- EDA: outbox 기반 이벤트 발행 흐름 확립
## 데모/실습
- 테이블/엔티티 확인: `order-orchestrator/src/main/java/.../OutboxMessage.java`
- 상태 업데이트 로직 확인: `order-orchestrator/.../OutboxMessageStatusJdbcAdapter.java`

## 코드 발췌 및 설명
- `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/OutboxMessagePersistenceAdapter.java`: outbox_message 상태 업데이트 포트 구현
```java
    @Override
    public void updateSagaStatus(String orderId, OrderSagaStatus status) {
        int updated = outboxMessageJpaRepository.updateSagaStatus(orderId, status, LocalDateTime.now());
        if (updated == 0) {
            throw new IllegalArgumentException("Outbox message not found: " + orderId);
        }
    }
```
- 왜 필요한가: outbox 상태 업데이트의 실패 처리를 보여줘, 이벤트 신뢰성의 근거를 설명할 수 있다.

## 커밋 상세
### d95cb17 outbox_message MSA 상태 저장 로직 추가
- 변경 요약: outbox_message MSA 상태 저장 로직 추가
- 핵심 로직: API 엔드포인트 처리 흐름
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`, `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/OutboxMessagePersistenceAdapter.java`
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`
```diff
+import com.example.orderorchestrator.application.port.in.CreateOrderUseCase;
+import com.example.orderorchestrator.application.port.in.UpdateOutboxMessageUseCase;
+import com.example.orderorchestrator.domain.model.status.MSAStatus;
+    private final UpdateOutboxMessageUseCase updateOutboxMessageUseCase;
+                request.pointNumber(),
+            calls.add(reserveCoupon(request.couponNumber(), result.orderId()));
+            calls.add(reservePoint(request.pointNumber(), result.orderId()));
```
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/OutboxMessagePersistenceAdapter.java`
```diff
+import com.example.orderorchestrator.application.port.out.UpdateOutboxMessagePort;
+import com.example.orderorchestrator.domain.model.status.MSAStatus;
+import java.time.LocalDateTime;
+
+public class OutboxMessagePersistenceAdapter implements SaveOutboxMessagePort, UpdateOutboxMessagePort {
+
+    @Override
+    public void updateCouponStatus(String orderId, MSAStatus status) {
```

### 0d2221b outboxMessage에 pointStatus 컬럼 추가
- 변경 요약: outboxMessage에 pointStatus 컬럼 추가
- 핵심 로직: 사가 상태/메시지 저장 로직
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/OutboxMessagePersistenceAdapter.java`, `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/jpa/entity/OutboxMessageJpaEntity.java`
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/OutboxMessagePersistenceAdapter.java`
```diff
+                message.pointStatus(),
+                saved.getPointStatus(),
```
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/jpa/entity/OutboxMessageJpaEntity.java`
```diff
+    @Enumerated(EnumType.STRING)
+    @Column(name = "point_status", nullable = false)
+    private MSAStatus pointStatus;
+
+            MSAStatus pointStatus,
+        this.pointStatus = pointStatus;
+    public MSAStatus getPointStatus() { return pointStatus; }
```

### 982ec0a saga_status가 결과에 맞게 Reserved 또는 Compensating으로 업데이트 되도록 로직 수정
- 변경 요약: saga_status가 결과에 맞게 Reserved 또는 Compensating으로 업데이트 되도록 로직 수정
- 핵심 로직: 예약/확정/보상 API
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`, `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/OrderSagaPersistenceAdapter.java`
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`
```diff
+import com.example.orderorchestrator.application.port.in.UpdateOrderSagaStatusUseCase;
+import com.example.orderorchestrator.domain.model.status.OrderSagaStatus;
+    private final UpdateOrderSagaStatusUseCase updateOrderSagaStatusUseCase;
+                .then(Mono.fromRunnable(() -> updateSagaStatus(result.orderId(), OrderSagaStatus.Reserved)))
+                .onErrorResume(ex -> {
+                    updateSagaStatus(result.orderId(), OrderSagaStatus.Compensating);
+                    return Mono.error(ex);
+                })
```
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/OrderSagaPersistenceAdapter.java`
```diff
+import com.example.orderorchestrator.application.port.out.UpdateOrderSagaStatusPort;
+import com.example.orderorchestrator.domain.model.status.OrderSagaStatus;
+public class OrderSagaPersistenceAdapter implements SaveOrderSagaPort, UpdateOrderSagaStatusPort {
+
+    @Override
+    public void updateStatus(String orderId, OrderSagaStatus status) {
+        OrderSagaJpaEntity entity = orderSagaJpaRepository.findByOrderId(orderId)
+                .orElseThrow(() -> new IllegalArgumentException("Order saga not found: " + orderId));
```

### 0531530 updateSagaStatus 메서드 리팩터링
- 변경 요약: updateSagaStatus 메서드 리팩터링
- 핵심 로직: 구조/중복 리팩터링
- 구조 변화: 소비자 모듈 또는 이벤트 처리 흐름 확장
- 주요 파일: `docs/codex_log.md`, `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/adapter/out/persistence/OutboxMessageStatusJdbcAdapter.java`
- 코드 발췌: `docs/codex_log.md`
```diff
+## 27) K8s 포트포워딩 오류 및 outbox order_status 갱신
+- 사용자 요청: 05_msa_portforward.sh 실행 시 로그 파일이 read-only 경로로 떨어지는 오류 수정 요청.
+- Codex 응답: 스크립트에 ROOT_DIR 정의를 추가해 로그가 프로젝트 루트에 기록되도록 수정.
+- 사용자 요청: outbox_message의 saga_status가 Completed/Compensated가 될 때 order_status도 동일하게 업데이트.
+- Codex 응답: OutboxMessageStatusJdbcAdapter.updateSagaStatus에서 saga_status에 따라 order_status를 함께 갱신하도록 반영.
+
+### 28) updateSagaStatus 메서드 리팩터링
+OutboxMessageStatusJdbcAdapter.java 에서 하나의 메서드가 여러 역할을 하고 있는데, updateSagaCompetedStatus, updateSagaCompensatedStatus 처럼 각각의 메서드로 리팩토링 하자.
```
- 코드 발췌: `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/adapter/out/persistence/OutboxMessageStatusJdbcAdapter.java`
```diff
+    @Override
+    public void updateCompletedStatus(String orderId) {
+        jdbcTemplate.update(
+                "update outbox_message set saga_status = ?, order_status = ?, updated_at = ? where order_id = ?",
+                OrderSagaStatus.Completed.name(),
+                MSAStatus.Completed.name(),
+                Timestamp.valueOf(LocalDateTime.now()),
+                orderId
```

### d95cb17 outbox_message MSA 상태 저장 로직 추가
- 변경 요약: outbox_message MSA 상태 저장 로직 추가
- 핵심 로직: API 엔드포인트 처리 흐름
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`, `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/OutboxMessagePersistenceAdapter.java`
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`
```diff
+import com.example.orderorchestrator.application.port.in.CreateOrderUseCase;
+import com.example.orderorchestrator.application.port.in.UpdateOutboxMessageUseCase;
+import com.example.orderorchestrator.domain.model.status.MSAStatus;
+    private final UpdateOutboxMessageUseCase updateOutboxMessageUseCase;
+                request.pointNumber(),
+            calls.add(reserveCoupon(request.couponNumber(), result.orderId()));
+            calls.add(reservePoint(request.pointNumber(), result.orderId()));
```
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/OutboxMessagePersistenceAdapter.java`
```diff
+import com.example.orderorchestrator.application.port.out.UpdateOutboxMessagePort;
+import com.example.orderorchestrator.domain.model.status.MSAStatus;
+import java.time.LocalDateTime;
+
+public class OutboxMessagePersistenceAdapter implements SaveOutboxMessagePort, UpdateOutboxMessagePort {
+
+    @Override
+    public void updateCouponStatus(String orderId, MSAStatus status) {
```

### 0d2221b outboxMessage에 pointStatus 컬럼 추가
- 변경 요약: outboxMessage에 pointStatus 컬럼 추가
- 핵심 로직: 사가 상태/메시지 저장 로직
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/OutboxMessagePersistenceAdapter.java`, `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/jpa/entity/OutboxMessageJpaEntity.java`
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/OutboxMessagePersistenceAdapter.java`
```diff
+                message.pointStatus(),
+                saved.getPointStatus(),
```
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/jpa/entity/OutboxMessageJpaEntity.java`
```diff
+    @Enumerated(EnumType.STRING)
+    @Column(name = "point_status", nullable = false)
+    private MSAStatus pointStatus;
+
+            MSAStatus pointStatus,
+        this.pointStatus = pointStatus;
+    public MSAStatus getPointStatus() { return pointStatus; }
```

### 982ec0a saga_status가 결과에 맞게 Reserved 또는 Compensating으로 업데이트 되도록 로직 수정
- 변경 요약: saga_status가 결과에 맞게 Reserved 또는 Compensating으로 업데이트 되도록 로직 수정
- 핵심 로직: 예약/확정/보상 API
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`, `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/OrderSagaPersistenceAdapter.java`
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationController.java`
```diff
+import com.example.orderorchestrator.application.port.in.UpdateOrderSagaStatusUseCase;
+import com.example.orderorchestrator.domain.model.status.OrderSagaStatus;
+    private final UpdateOrderSagaStatusUseCase updateOrderSagaStatusUseCase;
+                .then(Mono.fromRunnable(() -> updateSagaStatus(result.orderId(), OrderSagaStatus.Reserved)))
+                .onErrorResume(ex -> {
+                    updateSagaStatus(result.orderId(), OrderSagaStatus.Compensating);
+                    return Mono.error(ex);
+                })
```
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/persistence/OrderSagaPersistenceAdapter.java`
```diff
+import com.example.orderorchestrator.application.port.out.UpdateOrderSagaStatusPort;
+import com.example.orderorchestrator.domain.model.status.OrderSagaStatus;
+public class OrderSagaPersistenceAdapter implements SaveOrderSagaPort, UpdateOrderSagaStatusPort {
+
+    @Override
+    public void updateStatus(String orderId, OrderSagaStatus status) {
+        OrderSagaJpaEntity entity = orderSagaJpaRepository.findByOrderId(orderId)
+                .orElseThrow(() -> new IllegalArgumentException("Order saga not found: " + orderId));
```

### 0531530 updateSagaStatus 메서드 리팩터링
- 변경 요약: updateSagaStatus 메서드 리팩터링
- 핵심 로직: 구조/중복 리팩터링
- 구조 변화: 소비자 모듈 또는 이벤트 처리 흐름 확장
- 주요 파일: `docs/codex_log.md`, `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/adapter/out/persistence/OutboxMessageStatusJdbcAdapter.java`
- 코드 발췌: `docs/codex_log.md`
```diff
+## 27) K8s 포트포워딩 오류 및 outbox order_status 갱신
+- 사용자 요청: 05_msa_portforward.sh 실행 시 로그 파일이 read-only 경로로 떨어지는 오류 수정 요청.
+- Codex 응답: 스크립트에 ROOT_DIR 정의를 추가해 로그가 프로젝트 루트에 기록되도록 수정.
+- 사용자 요청: outbox_message의 saga_status가 Completed/Compensated가 될 때 order_status도 동일하게 업데이트.
+- Codex 응답: OutboxMessageStatusJdbcAdapter.updateSagaStatus에서 saga_status에 따라 order_status를 함께 갱신하도록 반영.
+
+### 28) updateSagaStatus 메서드 리팩터링
+OutboxMessageStatusJdbcAdapter.java 에서 하나의 메서드가 여러 역할을 하고 있는데, updateSagaCompetedStatus, updateSagaCompensatedStatus 처럼 각각의 메서드로 리팩토링 하자.
```
- 코드 발췌: `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/adapter/out/persistence/OutboxMessageStatusJdbcAdapter.java`
```diff
+    @Override
+    public void updateCompletedStatus(String orderId) {
+        jdbcTemplate.update(
+                "update outbox_message set saga_status = ?, order_status = ?, updated_at = ? where order_id = ?",
+                OrderSagaStatus.Completed.name(),
+                MSAStatus.Completed.name(),
+                Timestamp.valueOf(LocalDateTime.now()),
+                orderId
```

# Order Saga 이벤트

## 토픽
- 이름: `order-saga-events`
- 설정 키: `order.saga.events.topic`

## 프로듀서
- 서비스: order-orchestrator
- 프로듀서 설정: `spring.kafka.producer.*`

## 메시지 키
- 키: `orderId`
- 목적: 주문 단위 순서를 단일 파티션에서 유지

## 페이로드 (JSON)
`OrderSagaEvent`에서 직렬화됨.

필드:
- `orderId` (string): 주문 식별자
- `sagaId` (string): 사가 식별자
- `type` (string): 이벤트 타입 enum
- `status` (string): 사가 상태 enum

### 이벤트 타입 enum
- `RESERVE_SUCCEEDED`
- `RESERVE_FAILED`

### 사가 상태 enum
- `InProgress`
- `Reserved`
- `Completed`
- `Compensating`
- `Compensated`

## 예시
```json
{
  "orderId": "ORDER-20250101-000001",
  "sagaId": "SAGA-20250101-000001",
  "type": "RESERVE_SUCCEEDED",
  "status": "Reserved"
}
```

## 컨슈머 참고
- 발행/소비 모두 `orderId`를 키로 사용.
- 메시지는 at-least-once로 처리하고, 컨슈머는 멱등성을 고려.
- 스키마 버전은 아직 포함하지 않음. 필요하면 `schemaVersion` 필드를 추가.

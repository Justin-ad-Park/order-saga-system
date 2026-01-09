# Order Saga System - 프로젝트 아키텍처

## 요약
Kafka 기반 Saga 패턴으로 주문을 오케스트레이션하는 멀티 모듈 프로젝트다. 주문 생성과 쿠폰/포인트 예약을 `order-orchestrator`가 처리하고, 이벤트는 Kafka로 발행된다. `order-saga-consumer`가 이벤트를 소비해 확정/보상 처리를 수행하며, 각 서비스는 독립 DB를 사용한다.

## 1. 프로젝트 내부 모듈 간 연결 관계
- `order-orchestrator` -> `coupon-service` (HTTP): 쿠폰 예약 요청
- `order-orchestrator` -> `point-service` (HTTP): 포인트 예약 요청
- `order-orchestrator` -> Kafka: `order-saga-events` 토픽에 saga 이벤트 발행
- `order-saga-consumer` -> Kafka: saga 이벤트 소비
- `order-saga-consumer` -> `order-orchestrator` DB: `order_saga` 조회
- `order-saga-consumer` -> `coupon-service` (HTTP): 쿠폰 확정/보상 호출
- `order-saga-consumer` -> `point-service` (HTTP): 포인트 확정/보상 호출
- `common` -> 전 모듈: 공통 상태/응답 타입 공유

## 2. 제공 API
### order-orchestrator
#### `POST /api/v1/orders` : 주문 saga 생성 및 예약 흐름 시작

요청 파라미터 (`CreateOrderRequest`)

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| couponNumber | String | 선택 | 쿠폰 번호 |
| pointNumber | String | 선택 | 포인트 번호 |
| paymentNumber | String | 필수 | 결제 번호 |
| paymentAmount | Long | 필수 | 결제 금액 (1 이상) |
| orderItems | List<OrderItemRequest> | 필수 | 주문 항목 목록 |

`OrderItemRequest`

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| itemNumber | String | 필수 | 상품 번호 |
| quantity | Integer | 필수 | 수량 (1 이상) |

응답 파라미터 (`CreateOrderResponse`)

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| orderId | String | 주문 ID |
| sagaId | String | Saga ID |
| status | String | Saga 상태 |

요청 JSON 샘플

```json
{
  "couponNumber": "CPN-INT-BOTH-001",
  "pointNumber": "PNT-INT-BOTH-001",
  "paymentNumber": "PAY-001",
  "paymentAmount": 15000,
  "orderItems": [
    { "itemNumber": "ITEM-001", "quantity": 2 },
    { "itemNumber": "ITEM-002", "quantity": 1 }
  ]
}
```

응답 JSON 샘플

```json
{
  "orderId": "ORDER-20240101-0001",
  "sagaId": "SAGA-20240101-0001",
  "status": "Reserved"
}
```

### coupon-service
#### `POST /api/v1/coupons/reserve` : 쿠폰 예약

요청 파라미터 (`ReserveCouponRequest`)

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| couponNumber | String | 필수 | 쿠폰 번호 |
| orderId | String | 필수 | 주문 ID |

응답 파라미터 (`ApiResponse<ReserveCouponResponse>`)

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| success | boolean | 성공 여부 |
| data | object | 성공 데이터 |
| error | object | 실패 시 에러 |

`data` (ReserveCouponResponse)

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| couponNumber | String | 쿠폰 번호 |
| status | String | 쿠폰 상태 |

`error` (ApiError)

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| code | String | 에러 코드 |
| message | String | 에러 메시지 |

요청 JSON 샘플

```json
{
  "couponNumber": "CPN-INT-BOTH-001",
  "orderId": "ORDER-20240101-0001"
}
```

응답 JSON 샘플

```json
{
  "success": true,
  "data": {
    "couponNumber": "CPN-INT-BOTH-001",
    "status": "RESERVED"
  },
  "error": null
}
```

#### `POST /api/v1/coupons/confirm` : 쿠폰 확정

요청 파라미터 (`ConfirmCouponRequest`)

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| couponNumber | String | 필수 | 쿠폰 번호 |
| orderId | String | 필수 | 주문 ID |

응답 파라미터 (`ApiResponse<ConfirmCouponResponse>`)

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| success | boolean | 성공 여부 |
| data | object | 성공 데이터 |
| error | object | 실패 시 에러 |

`data` (ConfirmCouponResponse)

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| couponNumber | String | 쿠폰 번호 |
| status | String | 쿠폰 상태 |

요청 JSON 샘플

```json
{
  "couponNumber": "CPN-INT-BOTH-001",
  "orderId": "ORDER-20240101-0001"
}
```

응답 JSON 샘플

```json
{
  "success": true,
  "data": {
    "couponNumber": "CPN-INT-BOTH-001",
    "status": "USED"
  },
  "error": null
}
```

#### `POST /api/v1/coupons/compensate` : 쿠폰 보상(취소)

요청 파라미터 (`CompensateCouponRequest`)

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| couponNumber | String | 필수 | 쿠폰 번호 |
| orderId | String | 필수 | 주문 ID |

응답 파라미터 (`ApiResponse<CompensateCouponResponse>`)

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| success | boolean | 성공 여부 |
| data | object | 성공 데이터 |
| error | object | 실패 시 에러 |

`data` (CompensateCouponResponse)

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| couponNumber | String | 쿠폰 번호 |
| status | String | 쿠폰 상태 |

요청 JSON 샘플

```json
{
  "couponNumber": "CPN-INT-BOTH-001",
  "orderId": "ORDER-20240101-0001"
}
```

응답 JSON 샘플

```json
{
  "success": true,
  "data": {
    "couponNumber": "CPN-INT-BOTH-001",
    "status": "AVAILABLE"
  },
  "error": null
}
```

### point-service
#### `POST /api/v1/points/reserve` : 포인트 예약

요청 파라미터 (`ReservePointRequest`)

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| pointNumber | String | 필수 | 포인트 번호 |
| orderId | String | 필수 | 주문 ID |

응답 파라미터 (`ApiResponse<ReservePointResponse>`)

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| success | boolean | 성공 여부 |
| data | object | 성공 데이터 |
| error | object | 실패 시 에러 |

`data` (ReservePointResponse)

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| pointNumber | String | 포인트 번호 |
| status | String | 포인트 상태 |

`error` (ApiError)

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| code | String | 에러 코드 |
| message | String | 에러 메시지 |

요청 JSON 샘플

```json
{
  "pointNumber": "PNT-INT-BOTH-001",
  "orderId": "ORDER-20240101-0001"
}
```

응답 JSON 샘플

```json
{
  "success": true,
  "data": {
    "pointNumber": "PNT-INT-BOTH-001",
    "status": "RESERVED"
  },
  "error": null
}
```

#### `POST /api/v1/points/confirm` : 포인트 확정

요청 파라미터 (`ConfirmPointRequest`)

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| pointNumber | String | 필수 | 포인트 번호 |
| orderId | String | 필수 | 주문 ID |

응답 파라미터 (`ApiResponse<ConfirmPointResponse>`)

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| success | boolean | 성공 여부 |
| data | object | 성공 데이터 |
| error | object | 실패 시 에러 |

`data` (ConfirmPointResponse)

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| pointNumber | String | 포인트 번호 |
| status | String | 포인트 상태 |

요청 JSON 샘플

```json
{
  "pointNumber": "PNT-INT-BOTH-001",
  "orderId": "ORDER-20240101-0001"
}
```

응답 JSON 샘플

```json
{
  "success": true,
  "data": {
    "pointNumber": "PNT-INT-BOTH-001",
    "status": "USED"
  },
  "error": null
}
```

#### `POST /api/v1/points/compensate` : 포인트 보상(취소)

요청 파라미터 (`CompensatePointRequest`)

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| pointNumber | String | 필수 | 포인트 번호 |
| orderId | String | 필수 | 주문 ID |

응답 파라미터 (`ApiResponse<CompensatePointResponse>`)

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| success | boolean | 성공 여부 |
| data | object | 성공 데이터 |
| error | object | 실패 시 에러 |

`data` (CompensatePointResponse)

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| pointNumber | String | 포인트 번호 |
| status | String | 포인트 상태 |

요청 JSON 샘플

```json
{
  "pointNumber": "PNT-INT-BOTH-001",
  "orderId": "ORDER-20240101-0001"
}
```

응답 JSON 샘플

```json
{
  "success": true,
  "data": {
    "pointNumber": "PNT-INT-BOTH-001",
    "status": "AVAILABLE"
  },
  "error": null
}
```

### account
#### `POST /accounts` : 계정 생성

요청 파라미터 (`CreateAccountRequest`)

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| accountNumber | String | 필수 | 계좌 번호 |
| name | String | 필수 | 예금주 |
| balance | long | 필수 | 초기 잔액 (0 이상) |

응답 파라미터 (`ApiResponse<AccountResponse>`)

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| success | boolean | 성공 여부 |
| data | object | 성공 데이터 |
| error | object | 실패 시 에러 |

`data` (AccountResponse)

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| accountNumber | String | 계좌 번호 |
| name | String | 예금주 |
| balance | long | 잔액 |

`error` (ApiError)

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| code | String | 에러 코드 |
| message | String | 에러 메시지 |

요청 JSON 샘플

```json
{
  "accountNumber": "ACC-0001",
  "name": "홍길동",
  "balance": 10000
}
```

응답 JSON 샘플

```json
{
  "success": true,
  "data": {
    "accountNumber": "ACC-0001",
    "name": "홍길동",
    "balance": 10000
  },
  "error": null
}
```

#### `POST /accounts/{accountNumber}/deposit` : 입금

요청 파라미터 (`AmountRequest`)

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| amount | long | 필수 | 금액 (0 이상) |

응답 파라미터 (`ApiResponse<AccountResponse>`)

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| success | boolean | 성공 여부 |
| data | object | 성공 데이터 |
| error | object | 실패 시 에러 |

`data` (AccountResponse)

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| accountNumber | String | 계좌 번호 |
| name | String | 예금주 |
| balance | long | 잔액 |

요청 JSON 샘플

```json
{
  "amount": 5000
}
```

응답 JSON 샘플

```json
{
  "success": true,
  "data": {
    "accountNumber": "ACC-0001",
    "name": "홍길동",
    "balance": 15000
  },
  "error": null
}
```

#### `POST /accounts/{accountNumber}/withdraw` : 출금

요청 파라미터 (`AmountRequest`)

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| amount | long | 필수 | 금액 (0 이상) |

응답 파라미터 (`ApiResponse<AccountResponse>`)

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| success | boolean | 성공 여부 |
| data | object | 성공 데이터 |
| error | object | 실패 시 에러 |

`data` (AccountResponse)

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| accountNumber | String | 계좌 번호 |
| name | String | 예금주 |
| balance | long | 잔액 |

요청 JSON 샘플

```json
{
  "amount": 3000
}
```

응답 JSON 샘플

```json
{
  "success": true,
  "data": {
    "accountNumber": "ACC-0001",
    "name": "홍길동",
    "balance": 12000
  },
  "error": null
}
```

#### `GET /accounts/{accountNumber}` : 계좌 조회

응답 파라미터 (`ApiResponse<AccountResponse>`)

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| success | boolean | 성공 여부 |
| data | object | 성공 데이터 |
| error | object | 실패 시 에러 |

`data` (AccountResponse)

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| accountNumber | String | 계좌 번호 |
| name | String | 예금주 |
| balance | long | 잔액 |

응답 JSON 샘플

```json
{
  "success": true,
  "data": {
    "accountNumber": "ACC-0001",
    "name": "홍길동",
    "balance": 12000
  },
  "error": null
}
```

## 3. 테이블 구조
### order-orchestrator DB (MySQL)
> JPA 엔티티 기반으로 추정 (DDL은 JPA가 관리).

`order_saga`

| 컬럼 | 타입 | PK | INDEX | 컬럼 속성 | NULL | 기본값 | 한글 설명 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | YES | PK | auto | NO | - | 내부 식별자 |
| order_id | VARCHAR(255) | NO | UNIQUE | - | NO | - | 주문 식별자 |
| saga_id | VARCHAR(255) | NO | UNIQUE | - | NO | - | saga 식별자 |
| coupon_number | VARCHAR(255) | NO | - | - | YES | - | 적용된 쿠폰 번호 |
| point_number | VARCHAR(255) | NO | - | - | YES | - | 적용된 포인트 번호 |
| payment_number | VARCHAR(255) | NO | - | - | YES | - | 결제 번호 |
| payment_amount | BIGINT | NO | - | - | YES | - | 결제 금액 |
| status | VARCHAR(255) | NO | - | Enum(OrderSagaStatus) | NO | - | 주문 saga 상태 |

OrderSagaStatus

| 값 | 한글명 | 설명 |
| --- | --- | --- |
| InProgress | 진행 중 | 주문 생성 및 예약 처리 중 |
| Reserved | 예약 완료 | 쿠폰/포인트 예약이 완료된 상태 |
| Completed | 완료 | 주문 확정 완료 |
| Failed | 실패 | 예약/결제 실패로 saga 실패 |
| Compensating | 보상 중 | 보상 트랜잭션 수행 중 |
| Compensated | 보상 완료 | 보상 처리 완료 |

`order_item`

| 컬럼 | 타입 | PK | INDEX | 컬럼 속성 | NULL | 기본값 | 한글 설명 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | YES | PK | auto | NO | - | 내부 식별자 |
| item_number | VARCHAR(255) | NO | - | - | NO | - | 상품 번호 |
| quantity | INT | NO | - | - | NO | - | 주문 수량 |
| order_saga_id | BIGINT | NO | INDEX | FK -> order_saga.id | YES | - | 주문 saga 참조 키 |

`outbox_message`

| 컬럼 | 타입 | PK | INDEX | 컬럼 속성 | NULL | 기본값 | 한글 설명 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| id | BIGINT | YES | PK | auto | NO | - | 내부 식별자 |
| order_id | VARCHAR(255) | NO | - | - | NO | - | 주문 식별자 |
| payload | LOB | NO | - | - | NO | - | 이벤트 페이로드 |
| coupon_status | VARCHAR(255) | NO | - | Enum(MSAStatus) | NO | - | 쿠폰 처리 상태 |
| point_status | VARCHAR(255) | NO | - | Enum(MSAStatus) | NO | - | 포인트 처리 상태 |
| order_status | VARCHAR(255) | NO | - | Enum(MSAStatus) | NO | - | 주문 처리 상태 |
| saga_status | VARCHAR(255) | NO | - | Enum(OrderSagaStatus) | NO | - | saga 상태 |
| created_at | DATETIME | NO | - | - | NO | - | 생성 시각 |
| updated_at | DATETIME | NO | - | - | NO | - | 수정 시각 |

MSAStatus

| 값 | 한글명 | 설명 |
| --- | --- | --- |
| NotUsed | 미사용 | 해당 MSA가 주문에 참여하지 않음 |
| InProgress | 진행 중 | 외부 요청 처리 중 |
| Reserved | 예약 | 자원 예약 완료 |
| Completed | 완료 | 확정 완료 |
| Failed | 실패 | 예약/확정 실패 |
| Compensated | 보상 완료 | 보상 처리 완료 |

### order-saga-consumer DB 접근
- `order_saga`를 조회해 `coupon_number`, `point_number` 사용

### coupon-service DB (MySQL)
`coupon_schema.sql` 기준

`coupon`

| 컬럼 | 타입 | PK | INDEX | 컬럼 속성 | NULL | 기본값 | 한글 설명 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| coupon_number | VARCHAR(255) | YES | PK | - | NO | - | 쿠폰 번호 |
| status | VARCHAR(255) | NO | - | Enum(CouponStatus) | NO | - | 쿠폰 상태 |
| issued_at | TIMESTAMP | NO | - | - | NO | CURRENT_TIMESTAMP | 발급 시각 |
| expired_at | TIMESTAMP | NO | - | - | NO | - | 만료 시각 |

CouponStatus

| 값 | 한글명 | 설명 |
| --- | --- | --- |
| AVAILABLE | 사용 가능 | 미사용 상태 |
| RESERVED | 예약 | 주문 생성 중 예약됨 |
| USED | 사용 완료 | 주문 확정으로 사용됨 |
| COMPENSATED | 보상 완료 | 보상 처리로 복원됨 |

### point-service DB (MySQL)
`point_schema.sql` 기준

`point`

| 컬럼 | 타입 | PK | INDEX | 컬럼 속성 | NULL | 기본값 | 한글 설명 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| point_number | VARCHAR(255) | YES | PK | - | NO | - | 포인트 번호 |
| status | VARCHAR(255) | NO | - | Enum(PointStatus) | NO | - | 포인트 상태 |
| issued_at | TIMESTAMP | NO | - | - | NO | CURRENT_TIMESTAMP | 발급 시각 |
| expired_at | TIMESTAMP | NO | - | - | NO | - | 만료 시각 |

PointStatus

| 값 | 한글명 | 설명 |
| --- | --- | --- |
| AVAILABLE | 사용 가능 | 미사용 상태 |
| RESERVED | 예약 | 주문 생성 중 예약됨 |
| USED | 사용 완료 | 주문 확정으로 사용됨 |
| COMPENSATED | 보상 완료 | 보상 처리로 복원됨 |

### account DB (H2 기본)
`account/src/main/resources/schema.sql` 기준

`account`

| 컬럼 | 타입 | PK | INDEX | 컬럼 속성 | NULL | 기본값 | 한글 설명 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| account_number | VARCHAR(64) | YES | PK | - | NO | - | 계좌 번호 |
| name | VARCHAR(255) | NO | - | - | NO | - | 예금주 |
| balance | BIGINT | NO | - | - | NO | - | 잔액 |
## 4. 사용 기술
- Java 17: 서비스 구현 언어. 멀티 모듈 전반의 기본 런타임.
- Spring Boot 3.3.x: 서비스 부트스트랩과 설정/의존성 관리.
- Spring Web (MVC): REST API 엔드포인트 제공.
- Spring WebFlux (WebClient): 외부 MSA 호출용 비동기 HTTP 클라이언트.
- Spring Data JPA: 오케스트레이터/컨슈머의 JPA 기반 영속성 관리.
- JDBC + MyBatis: 일부 모듈의 JDBC 접근 및 매퍼 기반 쿼리.
- Kafka (Spring Kafka): saga 이벤트 발행/소비.
- MySQL: MSA 운영용 데이터 저장소.
- H2: account 모듈의 로컬/테스트용 DB.
- Gradle 멀티 모듈: 빌드와 의존성 관리 구조.
- ArchUnit: 헥사고날 아키텍처 규칙 테스트.

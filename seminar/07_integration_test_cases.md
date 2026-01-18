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

## 코드 발췌 및 설명
- `order-orchestrator/src/test/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationIntegrationTest.java`: 조합별 주문 생성 통합 테스트
```java
    @Test
    void createOrder_withCouponAndPoint_shouldPersistOrderSaga_and_OutboxMessage() {
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
```
- 왜 필요한가: 조합별 시나리오가 실제로 테스트되는 지점을 보여줘, 요구사항 커버리지를 설명하기 좋다.

## 커밋 상세
### b058e04 Test Case 추가. 쿠폰만, 포인트만, 둘 다, 하나도 없음
- 변경 요약: Test Case 추가. 쿠폰만, 포인트만, 둘 다, 하나도 없음
- 핵심 로직: 테스트 케이스 확장
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `order-orchestrator/src/test/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationIntegrationTest.java`
- 변경 전/후 비교: `order-orchestrator/src/test/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationIntegrationTest.java`
- diff 스타일
```diff
@@ -51,6 +51,7 @@ class OrderOrchestrationIntegrationTest {
     private static ConfigurableApplicationContext pointContext;
     private static int pointPort;
 
+
     @AfterAll
     static void stopMSAService() {
         if (couponContext != null) {
@@ -108,11 +109,26 @@ class OrderOrchestrationIntegrationTest {
     }
 
     @Test
-    void createOrder_shouldPersistOrderSaga_and_OutboxMessage() {
+    void createOrder_withCouponAndPoint_shouldPersistOrderSaga_and_OutboxMessage() {
         // given: 주문 생성 요청 바디
         Map<String, Object> requestBody = Map.of(
```
- 코드 발췌: `order-orchestrator/src/test/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationIntegrationTest.java`
```diff
+
+    void createOrder_withCouponAndPoint_shouldPersistOrderSaga_and_OutboxMessage() {
+                "couponNumber", "CPN-BOTH-001",
+                "pointNumber", "PNT-BOTH-001",
+                "paymentNumber", "PAY-001",
+                "paymentAmount", 35000L,
+                "orderItems", List.of(
+                        Map.of("itemNumber", "ITEM-001", "quantity", 2),
```

### fc9cbda - 통합 테스트 리팩토링 - 쿠폰 실패, 포인트 성공 케이스 추가
- 변경 요약: - 통합 테스트 리팩토링 - 쿠폰 실패, 포인트 성공 케이스 추가
- 핵심 로직: 테스트 케이스 확장
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `order-orchestrator/src/test/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationIntegrationTest.java`
- 변경 전/후 비교: `order-orchestrator/src/test/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationIntegrationTest.java`
- diff 스타일
```diff
@@ -10,7 +10,6 @@ import com.example.orderorchestrator.adapter.out.persistence.jpa.entity.OrderSag
 import com.example.orderorchestrator.adapter.out.persistence.jpa.entity.OutboxMessageJpaEntity;
 import com.example.orderorchestrator.domain.model.status.MSAStatus;
 import com.example.orderorchestrator.domain.model.status.OrderSagaStatus;
-import org.junit.jupiter.api.AfterEach;
 import org.junit.jupiter.api.AfterAll;
 import org.junit.jupiter.api.Test;
 import org.springframework.beans.factory.annotation.Autowired;
@@ -18,16 +17,15 @@ import org.springframework.boot.test.context.SpringBootTest;
 import org.springframework.boot.test.web.client.TestRestTemplate;
 import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
 import org.springframework.http.*;
-import org.springframework.test.annotation.Commit;
 import org.springframework.test.context.ActiveProfiles;
 import org.springframework.test.context.DynamicPropertyRegistry;
 import org.springframework.test.context.DynamicPropertySource;
```
- 코드 발췌: `order-orchestrator/src/test/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationIntegrationTest.java`
```diff
+import java.util.Comparator;
+    // 쿠폰과 포인트 모두 예약 가능한 경우
+    // 쿠폰만 사용하는 경우
+    // 포인트만 사용하는 경우
+    // 쿠폰/포인트 없이 주문하는 경우
+    // 쿠폰은 이미 예약되어 실패하고, 포인트는 예약 가능한 경우
+    @Test
+    void createOrder_withReservedCouponAndAvailablePoint_shouldMarkCouponFailedAndPointReserved() {
```

### 177839d - 로컬 테스트 시 order-orchestrator의 기존 테스트 데이터 삭제되도록 추가
- 변경 요약: - 로컬 테스트 시 order-orchestrator의 기존 테스트 데이터 삭제되도록 추가
- 핵심 로직: 테스트 케이스 확장
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `order-orchestrator/scripts/run_local_msa.sh`, `order-orchestrator/scripts/stop_local_msa.sh`
- 변경 전/후 비교: `order-orchestrator/scripts/run_local_msa.sh`
- diff 스타일
```diff
@@ -2,6 +2,7 @@
 set -euo pipefail
 
 ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
+cd "$ROOT_DIR"
 
 "${ROOT_DIR}/gradlew" :coupon-service:bootRun \
   -Dspring.profiles.active=test \
```
- 코드 발췌: `order-orchestrator/scripts/run_local_msa.sh`
```diff
+cd "$ROOT_DIR"
```
- 코드 발췌: `order-orchestrator/scripts/stop_local_msa.sh`
```diff
+  lsof -ti tcp:8080 | xargs kill
+  ps aux | rg "gradle.*bootRun" | rg ":order-orchestrator" | awk '{print $2}' | xargs kill
```

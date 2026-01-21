# Chapter 9: 서킷 브레이커 동작 테스트 및 안정성 검증

이론적으로 서킷 브레이커를 구현했지만, 실제로 장애 상황에서 우리가 의도한 대로 동작하는지 검증하는 과정은 매우 중요합니다. 분산 시스템에서는 예측하지 못한 상호작용이 발생할 수 있기 때문입니다.

본 챕터에서는 `coupon-service`에 설정한 Istio 서킷 브레이커가 실제로 동작하는지 확인하기 위한 테스트 환경과 시나리오에 대해 알아봅니다.

## 1. 테스트 시나리오

우리의 목표는 `coupon-service`가 응답하지 않을 때, `order-saga-consumer`가 `coupon-service` 호출을 즉시 중단(fail fast)하는지를 확인하는 것입니다.

1.  **장애 유발:** `coupon-service`에 의도적으로 3초의 응답 지연(delay)을 설정합니다.
2.  **타임아웃 설정:** `order-saga-consumer`가 `coupon-service`를 호출할 때의 타임아웃을 2초로 설정합니다.
3.  **반복 호출:** `order-saga-consumer`가 `coupon-service`를 연속적으로 여러 번 호출하도록 합니다.
4.  **서킷 브레이커 동작 관찰:**
    *   초기 몇 번의 호출은 2초 타임아웃이 발생하며 실패합니다.
    *   Istio의 `DestinationRule`에 설정된 임계치(예: 3회 연속 실패)에 도달하면 서킷 브레이커가 **Open** 상태로 전환됩니다.
    *   이후의 호출은 2초를 기다리지 않고 **즉시 실패**합니다. API 호출의 응답 시간이 수 밀리초(ms) 단위로 줄어드는 것을 통해 서킷이 열렸음을 확인할 수 있습니다.
5.  **회복 관찰:**
    *   서킷이 열리고 설정된 시간이 지나면(예: 1분), 브레이커는 **Half-Open** 상태가 됩니다.
    *   이때 `coupon-service`의 응답 지연을 다시 0초로 정상화합니다.
    *   `order-saga-consumer`의 다음 테스트 호출이 성공하면, 서킷 브레이커는 **Closed** 상태로 복원됩니다.
    *   이후의 모든 호출은 다시 정상적으로 처리됩니다.

## 2. 주요 Git 이력

아래 커밋들은 서킷 브레이커 테스트 환경을 구축하고 검증하는 과정을 보여줍니다.

```
* 4b031ed | 2026-01-15 | *** Timeout Test용 강제 지연 로직을 ... Decorator 패턴으로 분리
* 987a667 | 2026-01-14 | 04_test_circuit_breaker.sh 정리 ...
* c4401c7 | 2026-01-13 | Istio 설치. 강제 타임아웃 테스트용 로직 추가...
```

## 3. 핵심 코드 스니펫

### 1) 서킷 브레이커 테스트 쉘 스크립트

`bin_istio_test/04_test_circuit_breaker.sh` 스크립트는 Istio 서킷 브레이커의 동작을 검증하기 위한 일련의 과정을 자동화합니다.

**`bin_istio_test/04_test_circuit_breaker.sh`**
```bash
#!/usr/bin/env bash
set -euo pipefail

# ... (환경 변수 및 헬퍼 함수 생략)

echo "==> [5/7] timeout 3회 연속 (circuit open 유도)"
for i in "${!COUPON_FORCE_DELAY_LIST[@]}"; do
  # ✅ 강제 지연 쿠폰 번호를 사용하여 타임아웃 유발
  post_order "timeout-$((i + 1))" "${COUPON_FORCE_DELAY_LIST[$i]}" "${POINT_FORCE_DELAY_LIST[$i]}"
done

echo "==> [6/7] 2초 대기 (circuit open 유지 예상)"
sleep 2
# ✅ 서킷이 열렸으므로 즉시 실패가 예상됨
post_order "after-2s" "${COUPON_CIRCUIT_OFF2}" "${POINT_CIRCUIT_OFF2}"

echo "==> [7/7] 총 15초 경과 후 호출 (circuit 정상 여부 확인)"
sleep 13 # baseEjectionTime (10s) + 3s 여유
# ✅ 서킷이 닫혔으므로 정상 동작 예상
post_order "after-15s" "${COUPON_CIRCUIT_OFF3}" "${POINT_CIRCUIT_OFF3}"
```
스크립트는 `post_order` 함수를 통해 `order-orchestrator`에 요청을 보내며, 이때 `COUPON_FORCE_DELAY_LIST`에 있는 쿠폰 번호를 사용하여 `coupon-service`에 지연을 유발합니다. 이를 통해 서킷 브레이커가 열리고 닫히는 과정을 관찰할 수 있습니다.

### 2) 서비스 강제 지연 로직 (Decorator 패턴)

`coupon-service`의 `ReserveCouponDelayDecorator`는 개발/테스트 환경에서 특정 쿠폰 번호에 대해 응답 지연을 강제하여 서킷 브레이커 테스트를 용이하게 합니다. 이는 실제 비즈니스 로직에 영향을 주지 않도록 Decorator 패턴으로 분리되었습니다.

**`coupon-service/.../application/service/ReserveCouponDelayDecorator.java`**
```java
@Service
@Primary // ✅ ReserveCouponUseCase 의 주 구현체보다 우선 순위가 높음
@Profile({"dev", "test"}) // ✅ dev, test 프로파일에서만 활성화
@RequiredArgsConstructor
public class ReserveCouponDelayDecorator implements ReserveCouponUseCase {

    private final ReserveCouponService delegate; // ✅ 실제 ReserveCouponService 를 위임

    @Value("${circuit-test.coupon.delay-enabled:false}")
    private boolean delayEnabled;
    @Value("${circuit-test.coupon.delay-prefix:}")
    private String delayPrefix;
    @Value("${circuit-test.coupon.delay-ms:0}")
    private long delayMs;

    @Override
    public void reserve(String couponNumber, String orderId) {
        maybeDelay(couponNumber); // ✅ 조건에 따라 지연 실행
        delegate.reserve(couponNumber, orderId); // ✅ 실제 비즈니스 로직 위임
    }

    private void maybeDelay(String couponNumber) {
        if (!delayEnabled) return;
        if (delayMs <= 0 || delayPrefix == null || delayPrefix.isBlank()) return;
        if (!couponNumber.startsWith(delayPrefix)) return; // ✅ 특정 쿠폰 번호에 대해서만 지연

        try {
            Thread.sleep(delayMs); // ✅ 지정된 시간만큼 스레드 지연
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Delay interrupted", ex);
        }
    }
}
```
`@Primary`와 `@Profile` 어노테이션을 통해 `dev`나 `test` 환경에서만 이 Decorator가 활성화되어 `ReserveCouponUseCase`의 기본 구현체(`ReserveCouponService`)를 대체합니다. `maybeDelay` 메서드는 특정 조건(쿠폰 번호 프리픽스 일치)에서 `Thread.sleep()`을 호출하여 의도적인 지연을 발생시킵니다.

---

## 여정의 마무리

지금까지 우리는 단일 서비스에서 시작하여, MSA로 확장하고, 이벤트 기반 아키텍처와 Saga 패턴을 도입하여 데이터 정합성을 확보했으며, 마지막으로 Istio 서킷 브레이커를 통해 시스템의 안정성과 복원력까지 강화하는 긴 여정을 함께했습니다.

이 과정을 통해 우리는 복잡한 분산 환경에서 발생하는 다양한 문제들을 어떻게 체계적으로 해결해 나가는지에 대한 실용적인 경험을 얻었습니다.
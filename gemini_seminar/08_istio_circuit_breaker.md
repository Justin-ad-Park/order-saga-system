# Chapter 8: Istio 서킷 브레이커로 안정성 강화하기

Saga 패턴으로 데이터의 최종 일관성을 보장했지만, 만약 특정 서비스(예: `point-service`)가 장애로 인해 매우 느려지거나 응답이 없다면 어떻게 될까요? `order-saga-consumer`는 `point-service`를 호출하며 계속해서 타임아웃이 발생할 때까지 기다려야 합니다. 이는 `order-saga-consumer`의 리소스를 고갈시키고, 전체 시스템의 반응성을 떨어뜨리는 원인이 됩니다.

본 챕터에서는 이러한 연쇄 장애를 방지하기 위해 **서비스 메시(Service Mesh)**인 **Istio**를 활용하여 **서킷 브레이커(Circuit Breaker)** 패턴을 어떻게 구현하는지 알아봅니다.

## 1. 서킷 브레이커 패턴이란?

전기 회로의 차단기(Circuit Breaker)에서 유래한 패턴으로, 장애가 발생한 서비스를 반복적으로 호출하는 것을 막아 시스템 전체를 보호하는 메커니즘입니다.

1.  **Closed (닫힘):** 평상시 상태. 모든 요청이 정상적으로 전달됩니다.
2.  **Open (열림):** 장애가 특정 임계치(예: 5초 타임아웃 3회 연속)를 초과하면, 서킷 브레이커가 열립니다. 이 상태에서는 해당 서비스로의 모든 호출이 즉시 실패 처리됩니다. 불필요한 호출을 시도하지 않으므로, 호출하는 쪽(caller)의 리소스를 보호하고 장애가 발생한 서비스가 복구될 시간을 벌어줍니다.
3.  **Half-Open (반-열림):** 서킷이 열리고 일정 시간이 지나면, 브레이커는 '반-열림' 상태로 전환됩니다. 이 상태에서 소수의 테스트 요청을 보내 서비스가 복구되었는지 확인합니다. 테스트 요청이 성공하면 서킷을 다시 'Closed' 상태로 바꾸고, 실패하면 다시 'Open' 상태로 돌아가 대기 시간을 갖습니다.

## 2. 왜 Istio를 사용하는가?

과거에는 Hystrix 같은 라이브러리를 사용하여 애플리케이션 코드 내부에 서킷 브레이커 로직을 직접 구현했습니다. 하지만 이 방식은 코드 복잡도 증가, 언어 종속성, 관리의 어려움 등 여러 단점이 있습니다.

**Istio**와 같은 서비스 메시는 애플리케이션 코드 변경 없이, **사이드카 프록시(Sidecar Proxy)**를 통해 네트워크 레벨에서 서킷 브레이커 기능을 투명하게 제공합니다. 개발자는 비즈니스 로직에만 집중할 수 있으며, 서킷 브레이커 설정은 쿠버네티스 YAML 파일(`DestinationRule`)로 중앙에서 관리할 수 있습니다.

## 3. 주요 Git 이력

아래 커밋들은 Istio를 설치하고 `coupon-service`에 대한 서킷 브레이커를 설정하는 과정을 보여줍니다.
```
* 4b031ed | 2026-01-15 | Timeout Test용 강제 지연 로직을 ... Decorator 패턴으로 분리
* c4401c7 | 2026-01-13 | Istio 설치. 강제 타임아웃 테스트용 로직 추가...
* 327490d | 2026-01-12 | istio 설치 및 실행
```

## 4. 핵심 코드 스니펫

### Istio DestinationRule을 이용한 서킷 브레이커 설정

`c4401c7` 커밋에서 추가된 `circuit-breaker.yaml` 파일 내 `DestinationRule`은 `coupon-service`로 향하는 트래픽에 대한 서킷 브레이커 정책을 정의합니다.

**`bin_k8s/istio/config/circuit-breaker.yaml`**
```yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: coupon-service-dr # ✅ DestinationRule 의 이름
  namespace: msa
spec:
  host: coupon-service.msa.svc.cluster.local # ✅ 대상 서비스의 DNS 호스트
  trafficPolicy:
    outlierDetection: # ✅ 서킷 브레이커 (Outlier Detection) 설정
      consecutive5xxErrors: 3 # 연속 3번의 5xx 에러 발생 시
      interval: 5s            # 5초 간격으로 에러를 체크
      baseEjectionTime: 10s   # 서킷이 열렸을 때 최소 10초 동안 대기
      maxEjectionPercent: 100 # 모든 인스턴스를 강퇴 가능 (즉, 서킷 완전 개방)
---
# ... (다른 서비스들의 DestinationRule 및 VirtualService 설정)
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: coupon-service-vs
  namespace: msa
spec:
  hosts:
    - coupon-service.msa.svc.cluster.local
  http:
    - timeout: 2s # ✅ 2초 타임아웃 설정
      retries:
        attempts: 0
        retryOn: ""
      route:
        - destination:
            host: coupon-service.msa.svc.cluster.local
            port:
              number: 8081
```
이 `DestinationRule`은 `coupon-service`로의 요청에서 연속 3번의 5xx 에러가 발생하면 서킷을 열고, 10초 동안 해당 서비스로의 트래픽을 차단합니다. 또한 `VirtualService`를 통해 `coupon-service`로 가는 요청에 2초의 타임아웃을 설정하여, 지연이 발생했을 때 빠르게 실패를 감지하도록 합니다.

---
이제 우리 시스템은 특정 서비스의 장애가 다른 서비스로 전파되는 것을 막는 강력한 방어막을 갖추게 되었습니다. 하지만 이 서킷 브레이커가 정말 우리가 의도한 대로 동작하는지 어떻게 확인할 수 있을까요? 다음 마지막 챕터에서는 서킷 브레이커를 테스트하는 환경을 구축하고, 실제 동작을 검증하는 과정을 살펴보겠습니다.
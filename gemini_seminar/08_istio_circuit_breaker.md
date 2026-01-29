# Chapter 8: Istio 서킷 브레이커로 안정성 강화

## 1. 개요: 장애 전파 방지와 서킷 브레이커 패턴

이전 챕터에서 우리는 Saga 보상 트랜잭션과 멱등성 구현을 통해 분산 트랜잭션의 일관성을 확보했습니다. 하지만 특정 서비스에 과부하가 걸리거나 장애가 발생했을 때, 이 장애가 다른 서비스로 연쇄적으로 전파되어 시스템 전체가 마비되는 **캐스케이딩 장애(Cascading Failure)**는 여전히 큰 위협입니다. 이러한 문제를 해결하고 시스템의 외부적인 장애 상황에 대한 복원력(Resilience)을 강화하기 위해 **서킷 브레이커(Circuit Breaker) 패턴**을 도입합니다.

본 챕터에서는 서비스 메시(Service Mesh)인 **Istio**를 활용하여 서킷 브레이커를 어떻게 구현하는지 알아봅니다. Istio는 애플리케이션 코드 변경 없이 네트워크 레벨에서 장애 격리 및 회복 기능을 구성할 수 있도록 지원합니다.

### 핵심 학습 목표
*   서킷 브레이커 패턴의 필요성과 동작 원리를 이해합니다.
*   서비스 메시 Istio가 MSA 환경에서 제공하는 기능과 서킷 브레이커 구현 방식을 학습합니다.
*   Istio `DestinationRule` 및 `VirtualService`를 사용하여 서킷 브레이커 정책을 설정하는 방법을 익힙니다.
*   쿠버네티스 환경에 Istio를 설치하고 서킷 브레이커 설정을 적용하는 방법을 실습합니다.

## 2. 서킷 브레이커 패턴과 Istio

**서킷 브레이커 패턴:**
전기 회로의 차단기(Circuit Breaker)처럼, 특정 서비스에 대한 호출이 지속적으로 실패할 경우 해당 서비스로의 트래픽을 일시적으로 차단하여 더 이상 실패 요청을 보내지 않도록 합니다. 이는 실패한 서비스가 복구될 시간을 벌어주고, 호출하는 서비스가 리소스를 낭비하지 않도록 보호하며, 캐스케이딩 장애를 방지합니다.

서킷 브레이커는 일반적으로 세 가지 상태를 가집니다:
*   **Closed (닫힘):** 정상 상태. 요청을 서비스로 전달합니다.
*   **Open (열림):** 장애 감지 시 요청을 차단하고 즉시 실패 응답을 반환합니다.
*   **Half-Open (반쯤 열림):** 일정 시간 후 소수의 테스트 요청만 서비스로 보내 성공 여부를 확인합니다. 성공하면 Closed로, 실패하면 다시 Open으로 전환됩니다.

**서비스 메시 Istio:**
Istio는 마이크로서비스 간의 통신을 제어하고 가시성을 제공하며 보안을 강화하는 오픈소스 서비스 메시 플랫폼입니다. Istio는 애플리케이션 코드 변경 없이 트래픽 관리(라우팅, 로드 밸런싱), 정책 적용(권한 부여, 속도 제한), 관측성(모니터링, 추적), 그리고 **장애 복원력(서킷 브레이커, 재시도, 타임아웃)**과 같은 기능을 제공합니다.

Istio는 주로 `Envoy Proxy`를 각 서비스 파드에 사이드카(Sidecar) 컨테이너로 주입하여 동작합니다. 모든 인바운드/아웃바운드 트래픽은 이 Envoy Proxy를 통해 흐르며, Istio Control Plane에서 설정한 정책들이 Envoy에 의해 적용됩니다.

## 3. Istio 및 서킷 브레이커 관련 Git 이력

Istio 설치, 서킷 브레이커 설정 및 관련 스크립트 추가와 관련된 주요 Git 커밋입니다.

| 커밋 ID | 날짜 | 주요 변경 요약 |
|---|---|---|
| `327490d` | 2026-01-14 | Istio 설치 및 실행 스크립트 추가 |
| `c4401c7` | 2026-01-14 | Istio 설치. 강제 타임아웃 테스트용 로직 추가 |
| `6161467` | 2026-01-14 | Istio 설치 경로 및 `yaml` 설정 파일 분리 |
| `4b031ed` | 2026-01-15 | 타임아웃 테스트용 강제 지연 로직을 Decorator 패턴으로 분리 |

**(실습 가이드: Git 커밋 확인)**
1.  `git checkout 327490d` 명령어로 해당 커밋 시점으로 이동하여 `bin_k8s/10_install_istio.sh` 파일의 초기 내용을 확인해 보세요.
2.  `git diff 327490d~1 6161467` 명령어로 Istio 설정 파일(`circuit-breaker.yaml`)이 분리된 변경사항을 확인할 수 있습니다.

## 4. 핵심 코드 스니펫: Istio 서킷 브레이커 설정

### 4.1. Istio 설치 스크립트 `10_install_istio.sh`

Istio `istioctl` CLI 도구를 설치하고, 이를 사용하여 Istio를 쿠버네티스 클러스터에 설치합니다.

**`bin_k8s/10_install_istio.sh`**
```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ISTIO_VERSION="${ISTIO_VERSION:-1.20.2}" # 사용할 Istio 버전
ISTIO_PROFILE="${ISTIO_PROFILE:-demo}" # 설치 프로파일 (demo, default, minimal 등)
ISTIO_DIR="${ROOT_DIR}/bin_k8s/istio/dist/istio-${ISTIO_VERSION}" # Istio 설치 디렉토리

# istioctl 설치 확인 및 설치
if command -v istioctl >/dev/null 2>&1; then
  echo "istioctl is already installed."
else
  echo "Downloading istioctl ${ISTIO_VERSION}..."
  curl -L https://istio.io/downloadIstio | ISTIO_VERSION="${ISTIO_VERSION}" sh - # Istio 공식 스크립트로 istioctl 다운로드
  mkdir -p "${ROOT_DIR}/bin_k8s/istio/dist" # 설치 디렉토리 생성
  mv "${ROOT_DIR}/istio-${ISTIO_VERSION}" "${ISTIO_DIR}" # 다운로드 받은 파일 이동
  export PATH="${ISTIO_DIR}/bin:${PATH}" # PATH 환경 변수 설정
  echo "istioctl installed at ${ISTIO_DIR}/bin/istioctl"
fi

echo "Installing Istio (profile=${ISTIO_PROFILE})..."
istioctl install --set profile="${ISTIO_PROFILE}" -y # 지정된 프로파일로 Istio 설치

echo "Istio CRDs (Custom Resource Definitions) 확인:"
kubectl get crd | grep 'istio' || true # Istio 관련 CRD 목록 확인
```
**설명:** 이 스크립트는 `istioctl` 명령어를 설치하고, `istioctl install` 명령을 통해 Istio를 쿠버네티스 클러스터에 배포합니다. `demo` 프로파일은 개발 및 테스트 환경에 적합한 기본 설정을 포함합니다.

### 4.2. 서킷 브레이커 설정 적용 스크립트 `09_apply_istio_cb.sh`

Istio가 설치된 후, 특정 네임스페이스에 Envoy 사이드카 주입을 활성화하고 서킷 브레이커 정책을 적용합니다.

**`bin_k8s/09_apply_istio_cb.sh`**
```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Istio 설치 여부 확인
if ! kubectl get ns istio-system >/dev/null 2>&1; then
  echo "Istio not detected; installing..."
  "${ROOT_DIR}/bin_k8s/10_install_istio.sh" # Istio가 설치되어 있지 않으면 설치
fi

# msa 네임스페이스 생성 및 istio-injection 활성화
kubectl get ns msa >/dev/null 2>&1 || kubectl create namespace msa
kubectl label namespace msa istio-injection=enabled --overwrite # msa 네임스페이스에 사이드카 자동 주입 라벨 추가

echo "==> Istio Circuit Breaker 설정 적용"
kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/istio/config/circuit-breaker.yaml" # 서킷 브레이커 정책 적용
```
**설명:** `istio-injection=enabled` 라벨을 `msa` 네임스페이스에 추가하면, 이 네임스페이스에 배포되는 모든 파드에 Envoy 사이드카 프록시가 자동으로 주입됩니다. 그 후 `circuit-breaker.yaml`에 정의된 정책들이 Envoy 프록시에 의해 적용됩니다.

### 4.3. Istio 서킷 브레이커 정책 `circuit-breaker.yaml`

이 YAML 파일은 `DestinationRule`과 `VirtualService` 리소스를 사용하여 `order-orchestrator`, `coupon-service`, `point-service`에 서킷 브레이커 및 타임아웃 정책을 설정합니다.

**`bin_k8s/istio/config/circuit-breaker.yaml`**
```yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: order-orchestrator-dr
  namespace: msa
spec:
  host: order-orchestrator.msa.svc.cluster.local # 대상 서비스의 호스트명
  trafficPolicy:
    outlierDetection: # 아웃라이어 감지 (서킷 브레이커 설정)
      consecutive5xxErrors: 3  # 5xx 응답 에러가 3회 연속 발생 시
      interval: 5s            # 5초 간격으로 실패 감지
      baseEjectionTime: 10s   # 격리(ejection) 유지 시간 (10초)
      maxEjectionPercent: 100 # 격리할 최대 인스턴스 비율 (100% = 모든 인스턴스)
---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: order-orchestrator-vs
  namespace: msa
spec:
  hosts:
    - order-orchestrator.msa.svc.cluster.local
  http:
    - timeout: 2s            # HTTP 요청 타임아웃 (2초)
      retries:
        attempts: 0          # 재시도 비활성화 (캐스케이딩 방지를 위해)
        retryOn: ""          # 재시도 조건 없음
      route:
        - destination:
            host: order-orchestrator.msa.svc.cluster.local
            port:
              number: 8099
---
# coupon-service에 대한 DestinationRule (order-orchestrator와 동일한 설정)
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: coupon-service-dr
  namespace: msa
spec:
  host: coupon-service.msa.svc.cluster.local
  trafficPolicy:
    outlierDetection:
      consecutive5xxErrors: 3
      interval: 5s
      baseEjectionTime: 10s
      maxEjectionPercent: 100
---
# coupon-service에 대한 VirtualService (timeout 2s)
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: coupon-service-vs
  namespace: msa
spec:
  hosts:
    - coupon-service.msa.svc.cluster.local
  http:
    - timeout: 2s
      retries:
        attempts: 0
        retryOn: ""
      route:
        - destination:
            host: coupon-service.msa.svc.cluster.local
            port:
              number: 8081
---
# point-service에 대한 DestinationRule (order-orchestrator와 동일한 설정)
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: point-service-dr
  namespace: msa
spec:
  host: point-service.msa.svc.cluster.local
  trafficPolicy:
    outlierDetection:
      consecutive5xxErrors: 3
      interval: 5s
      baseEjectionTime: 10s
      maxEjectionPercent: 100
---
# point-service에 대한 VirtualService (timeout 2s)
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: point-service-vs
  namespace: msa
spec:
  hosts:
    - point-service.msa.svc.cluster.local
  http:
    - timeout: 2s
      retries:
        attempts: 0
        retryOn: ""
      route:
        - destination:
            host: point-service.msa.svc.cluster.local
            port:
              number: 8082
```
**설명:**
*   **`DestinationRule`:** 특정 서비스(여기서는 `order-orchestrator`, `coupon-service`, `point-service`)에 대한 아웃라이어 감지(Outlier Detection) 설정을 정의합니다. 이는 서킷 브레이커의 핵심 설정으로, `consecutive5xxErrors` (연속 5xx 에러 횟수), `interval` (감지 간격), `baseEjectionTime` (격리 시간), `maxEjectionPercent` (격리 비율) 등을 설정하여 서킷 브레이커의 동작 방식을 제어합니다.
*   **`VirtualService`:** 서비스에 대한 트래픽 라우팅 규칙을 정의하지만, 여기서는 추가적으로 `timeout`과 `retries` 정책을 설정합니다. `timeout: 2s`는 해당 서비스로의 요청이 2초 안에 응답을 받지 못하면 타임아웃으로 처리함을 의미합니다. `attempts: 0`은 재시도를 비활성화하여 실패가 즉시 전파되도록 하여 캐스케이딩 장애를 방지합니다.

## 5. 실습 체크포인트

### 5.1. Istio 설치 및 서킷 브레이커 설정 적용

1.  **쿠버네티스 클러스터 준비:** MiniKube 또는 Docker Desktop의 Kubernetes를 실행합니다.
2.  **`bin_k8s/09_apply_istio_cb.sh` 실행:**
    *   프로젝트 루트에서 `./bin_k8s/09_apply_istio_cb.sh`를 실행합니다. 이 스크립트는 Istio가 설치되어 있지 않으면 자동으로 설치하고, `msa` 네임스페이스에 `istio-injection=enabled` 라벨을 추가한 후 `circuit-breaker.yaml` 정책을 적용합니다.
    *   **예상 결과:** Istio가 성공적으로 설치되고, CRD가 생성되며, `msa` 네임스페이스에 라벨이 적용되고, `DestinationRule` 및 `VirtualService` 리소스가 생성됩니다.
3.  **Istio 사이드카 주입 확인:**
    *   `msa` 네임스페이스에 배포된 서비스들(`coupon-service`, `point-service`, `order-orchestrator`, `order-saga-consumer`, `kafka`, `mysql` 등)의 파드를 재시작하거나 새로 배포하여 Envoy 사이드카가 주입되도록 합니다.
    *   `kubectl get pods -n msa` 명령어로 파드 목록을 확인했을 때, 각 애플리케이션 파드의 `READY` 상태가 `2/2` (애플리케이션 컨테이너 + Envoy 사이드카 컨테이너)로 표시되는지 확인합니다.
4.  **Istio CRD 및 정책 확인:**
    *   `kubectl get destinationrule -n msa`
    *   `kubectl get virtualservice -n msa`
    위 명령어를 실행하여 서킷 브레이커 정책들이 정상적으로 적용되었는지 확인합니다.

---
Istio를 통해 서킷 브레이커를 설정함으로써, 우리는 분산 시스템의 복원력을 한층 강화했습니다. 이제 다음 챕터에서는 실제로 서킷 브레이커가 장애 상황에서 어떻게 동작하고, 시스템이 어떻게 안정적으로 복구되는지 테스트 시나리오를 통해 검증하는 방법을 알아봅니다.
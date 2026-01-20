# 11-1. Istio 설치 및 Circuit Breaker 설정

## 목표
Istio 설치 흐름과 Circuit Breaker 적용 방식을 이해한다.

## 설치 흐름
1) `istioctl` 설치 및 Istio 설치
2) `msa` 네임스페이스에 사이드카 주입 활성화
3) Circuit Breaker 설정 적용
4) 모니터링 애드온 설치 및 포트포워드

## Istio 설치 스크립트
`bin_k8s/10_install_istio.sh`
```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ISTIO_VERSION="${ISTIO_VERSION:-1.20.2}"
ISTIO_PROFILE="${ISTIO_PROFILE:-demo}"
ISTIO_DIR="${ROOT_DIR}/bin_k8s/istio/dist/istio-${ISTIO_VERSION}"

if command -v istioctl >/dev/null 2>&1; then
  echo "istioctl already installed: $(command -v istioctl)"
else
  echo "Downloading istioctl ${ISTIO_VERSION}..."
  curl -L https://istio.io/downloadIstio | ISTIO_VERSION="${ISTIO_VERSION}" sh -
  mkdir -p "${ROOT_DIR}/bin_k8s/istio/dist"
  mv "${ROOT_DIR}/istio-${ISTIO_VERSION}" "${ISTIO_DIR}"
  export PATH="${ISTIO_DIR}/bin:${PATH}"
  echo "istioctl installed at ${ISTIO_DIR}/bin/istioctl"
fi

echo "Installing Istio (profile=${ISTIO_PROFILE})..."
istioctl install --set profile="${ISTIO_PROFILE}" -y

echo "Istio CRDs:"
kubectl get crd | rg 'istio|virtualservice|destinationrule' || true
```

## Circuit Breaker 적용 스크립트
`bin_k8s/09_apply_istio_cb.sh`
```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if ! kubectl get ns istio-system >/dev/null 2>&1; then
  echo "Istio not detected; installing..."
  "${ROOT_DIR}/bin_k8s/10_install_istio.sh"
fi

kubectl get ns msa >/dev/null 2>&1 || kubectl create namespace msa
kubectl label namespace msa istio-injection=enabled --overwrite
kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/istio/config/circuit-breaker.yaml"
```

## Circuit Breaker 설정
- `bin_k8s/istio/config/circuit-breaker.yaml`
```yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: order-orchestrator-dr
  namespace: msa
spec:
  host: order-orchestrator.msa.svc.cluster.local
  trafficPolicy:
    outlierDetection:
      consecutive5xxErrors: 3  # 5xx 연속 실패 3회 시 격리
      interval: 5s            # 실패 감지 간격
      baseEjectionTime: 10s   # 격리 유지 시간
      maxEjectionPercent: 100 # 격리 비율 상한
---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: coupon-service-dr
  namespace: msa
spec:
  host: coupon-service.msa.svc.cluster.local
  trafficPolicy:
    outlierDetection:
      consecutive5xxErrors: 3  # 5xx 연속 실패 3회 시 격리
      interval: 5s            # 실패 감지 간격
      baseEjectionTime: 10s   # 격리 유지 시간
      maxEjectionPercent: 100 # 격리 비율 상한
---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: point-service-dr
  namespace: msa
spec:
  host: point-service.msa.svc.cluster.local
  trafficPolicy:
    outlierDetection:
      consecutive5xxErrors: 3  # 5xx 연속 실패 3회 시 격리
      interval: 5s            # 실패 감지 간격
      baseEjectionTime: 10s   # 격리 유지 시간
      maxEjectionPercent: 100 # 격리 비율 상한
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
    - timeout: 2s            # 응답 대기 시간 상한
      retries:
        attempts: 0          # 재시도 비활성화
        retryOn: ""          # 재시도 조건 없음
      route:
        - destination:
            host: order-orchestrator.msa.svc.cluster.local
            port:
              number: 8099
---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: coupon-service-vs
  namespace: msa
spec:
  hosts:
    - coupon-service.msa.svc.cluster.local
  http:
    - timeout: 2s            # 응답 대기 시간 상한
      retries:
        attempts: 0          # 재시도 비활성화
        retryOn: ""          # 재시도 조건 없음
      route:
        - destination:
            host: coupon-service.msa.svc.cluster.local
            port:
              number: 8081
---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: point-service-vs
  namespace: msa
spec:
  hosts:
    - point-service.msa.svc.cluster.local
  http:
    - timeout: 2s            # 응답 대기 시간 상한
      retries:
        attempts: 0          # 재시도 비활성화
        retryOn: ""          # 재시도 조건 없음
      route:
        - destination:
            host: point-service.msa.svc.cluster.local
            port:
              number: 8082
```


## 모니터링 애드온 설치 및 포트포워드
`bin_k8s/11_start_istio_monitoring.sh`
```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ISTIO_VERSION="${ISTIO_VERSION:-1.20.2}"
ISTIO_DIR="${ROOT_DIR}/bin_k8s/istio/dist/istio-${ISTIO_VERSION}"

kill_port() {
  local port="$1"
  if command -v lsof >/dev/null 2>&1; then
    local pids
    pids="$(lsof -ti tcp:"${port}" || true)"
    if [[ -n "${pids}" ]]; then
      echo "${pids}" | xargs kill
    fi
  else
    ps aux | rg "tcp:${port}" | awk '{print $2}' | xargs kill || true
  fi
}

if ! kubectl get ns istio-system >/dev/null 2>&1; then
  echo "Istio not detected; installing..."
  "${ROOT_DIR}/bin_k8s/10_install_istio.sh"
fi

if [[ ! -d "${ISTIO_DIR}/samples/addons" ]]; then
  echo "Istio addons not found; installing Istio to fetch samples..."
  "${ROOT_DIR}/bin_k8s/10_install_istio.sh"
fi

echo "Applying Istio addons (kiali, grafana, prometheus, jaeger)..."
kubectl apply -f "${ISTIO_DIR}/samples/addons"

kubectl -n istio-system rollout status deployment/kiali
kubectl -n istio-system rollout status deployment/grafana
kubectl -n istio-system rollout status deployment/prometheus
kubectl -n istio-system rollout status deployment/jaeger

echo "Restarting Istio dashboard port-forwards..."
kill_port 20001
kill_port 3000
kill_port 9090
kill_port 16686

kubectl -n istio-system port-forward svc/kiali 20001:20001 > "${ROOT_DIR}/kiali-port-forward.log" 2>&1 &
kubectl -n istio-system port-forward svc/grafana 3000:3000 > "${ROOT_DIR}/grafana-port-forward.log" 2>&1 &
kubectl -n istio-system port-forward svc/prometheus 9090:9090 > "${ROOT_DIR}/prometheus-port-forward.log" 2>&1 &
kubectl -n istio-system port-forward svc/tracing 16686:80 > "${ROOT_DIR}/jaeger-port-forward.log" 2>&1 &

echo "Kiali:      http://localhost:20001"
echo "Grafana:    http://localhost:3000"
echo "Prometheus: http://localhost:9090"
echo "Jaeger:     http://localhost:16686"
```

## 확인 체크포인트
- `istio-system` 네임스페이스와 CRD가 생성되었는지 확인
- `msa` 네임스페이스에 `istio-injection=enabled` 라벨이 붙었는지 확인
- `DestinationRule`, `VirtualService`가 `msa` 네임스페이스에 적용되었는지 확인
- Kiali/Grafana/Prometheus/Jaeger 대시보드 포트포워드 확인

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

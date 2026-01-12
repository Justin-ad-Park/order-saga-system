#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

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

# 1) MSA 포트포워드 정리
echo "==> [1/7] MSA 포트포워드 정리 (8099/8091/8092)"
kill_port 8099
kill_port 8091
kill_port 8092

# 2) MySQL/Kafka 포트포워드 정리
echo "==> [2/7] MySQL/Kafka 포트포워드 정리 (3307/9094)"
kill_port 3307
kill_port 9094

# 3) 네임스페이스/MySQL/Kafka/Istio 적용
echo "==> [3/7] 네임스페이스/MySQL/Kafka/Istio 적용"
kubectl get ns msa >/dev/null 2>&1 || kubectl create namespace msa
bash "${ROOT_DIR}/bin_k8s/09_apply_istio_cb.sh"
kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/mysql.yaml"
kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/kafka.yaml"
kubectl -n msa rollout status deployment/kafka

# 4) MySQL/Kafka 포트포워드 재시작
echo "==> [4/7] MySQL/Kafka 포트포워드 재시작"
kubectl -n msa port-forward svc/mysql 3307:3306 > "${ROOT_DIR}/mysql-port-forward.log" 2>&1 &
kubectl -n msa port-forward svc/kafka 9094:9094 > "${ROOT_DIR}/kafka-port-forward.log" 2>&1 &

# 5) MSA 이미지 빌드 및 배포
echo "==> [5/7] MSA 이미지 빌드 및 배포"
"${ROOT_DIR}/coupon-service/scripts/deploy_k8s.sh"
"${ROOT_DIR}/point-service/scripts/deploy_k8s.sh"
"${ROOT_DIR}/order-orchestrator/scripts/deploy_k8s.sh"


# 6) MSA 재기동 및 포트포워드
echo "==> [6/7] MSA 재기동 및 포트포워드"
kubectl -n msa rollout restart deployment/coupon-service
kubectl -n msa rollout restart deployment/point-service
kubectl -n msa rollout restart deployment/order-orchestrator
kubectl -n msa rollout status deployment/order-orchestrator
kubectl -n msa port-forward svc/order-orchestrator 8099:8099 > "${ROOT_DIR}/order-port-forward.log" 2>&1 &
kubectl -n msa port-forward svc/coupon-service 8091:8081 > "${ROOT_DIR}/coupon-port-forward.log" 2>&1 &
kubectl -n msa port-forward svc/point-service 8092:8082 > "${ROOT_DIR}/point-port-forward.log" 2>&1 &

# 7) 로컬 Consumer 실행 (K8s 포트포워드 기준)
echo "==> [7/7] 로컬 Consumer 실행 (order-saga-consumer, k8s-local)"
"${ROOT_DIR}/gradlew" :order-saga-consumer:bootRun --args="--spring.profiles.active=k8s-local"

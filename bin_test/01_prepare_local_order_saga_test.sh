#!/usr/bin/env bash
set -euo pipefail

COMMON_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../bin_common" && pwd)"
# shellcheck disable=SC1091
source "${COMMON_DIR}/lib.sh"

# 1) 로컬 MSA 포트 정리
echo "==> [1/5] 로컬 MSA 포트 정리 (8080/8081/8082)"
kill_port 8080
kill_port 8081
kill_port 8082

# 2) MySQL/Kafka 포트포워드 정리
echo "==> [2/5] MySQL/Kafka 포트포워드 정리 (3307/9094)"
kill_port 3307
kill_port 9094

# 3) K8s MySQL/Kafka 적용 및 포트포워드 재시작
echo "==> [3/5] K8s MySQL/Kafka 적용 및 포트포워드 재시작"
kubectl get ns msa >/dev/null 2>&1 || kubectl create namespace msa
kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/mysql.yaml"
kubectl -n msa port-forward svc/mysql 3307:3306 > "${ROOT_DIR}/mysql-port-forward.log" 2>&1 &
kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/kafka.yaml"
kubectl -n msa rollout status deployment/kafka
kubectl -n msa port-forward svc/kafka 9094:9094 > "${ROOT_DIR}/kafka-port-forward.log" 2>&1 &

# 4) 로컬 MSA 재실행
echo "==> [4/5] 로컬 MSA 재실행 (coupon/point/order-orchestrator)"
cd "${ROOT_DIR}"
"${ROOT_DIR}/gradlew" :coupon-service:bootRun -Dspring.profiles.active=test &
"${ROOT_DIR}/gradlew" :point-service:bootRun -Dspring.profiles.active=test &
"${ROOT_DIR}/gradlew" :order-orchestrator:bootRun -Dspring.profiles.active=test &

# 5) 로컬 Consumer 실행
echo "==> [5/5] 로컬 Consumer 실행 (order-saga-consumer)"
"${ROOT_DIR}/gradlew" :order-saga-consumer:bootRun --args="--spring.profiles.active=test"

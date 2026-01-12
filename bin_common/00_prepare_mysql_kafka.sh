#!/usr/bin/env bash
set -euo pipefail

COMMON_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${COMMON_DIR}/lib.sh"

# 1) MySQL/Kafka 포트포워드 정리
echo "==> [2/5] MySQL/Kafka 포트포워드 정리 (3307/9094)"
kill_port 3307
kill_port 9094

# 2) K8s MySQL/Kafka 적용 및 포트포워드 재시작
echo "==> [3/5] K8s MySQL/Kafka 적용 및 포트포워드 재시작"
kubectl get ns msa >/dev/null 2>&1 || kubectl create namespace msa
kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/mysql.yaml"
kubectl -n msa port-forward svc/mysql 3307:3306 > "${ROOT_DIR}/mysql-port-forward.log" 2>&1 &
kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/kafka.yaml"
kubectl -n msa rollout status deployment/kafka
kubectl -n msa port-forward svc/kafka 9094:9094 > "${ROOT_DIR}/kafka-port-forward.log" 2>&1 &

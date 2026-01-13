#!/usr/bin/env bash
set -euo pipefail

COMMON_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${COMMON_DIR}/lib.sh"

ISTIO_ENABLED="${ISTIO_ENABLED:-false}"

# 1) MSA 포트포워드 정리
echo "==> [1/9] MSA 포트포워드 정리 (8099/8091/8092)"
kill_port 8099
kill_port 8091
kill_port 8092

# 2) MySQL/Kafka 포트포워드 정리
echo "==> [2/9] MySQL/Kafka 포트포워드 정리 (3307/9094)"
kill_port 3307
kill_port 9094

# 3) 네임스페이스/MySQL/Kafka
if [[ "${ISTIO_ENABLED}" == "true" ]]; then
  echo "==> [3/9] 네임스페이스/MySQL/Kafka/Istio 적용"
else
  echo "==> [3/9] 네임스페이스/MySQL/Kafka 적용"
fi
kubectl get ns msa >/dev/null 2>&1 || kubectl create namespace msa
if [[ "${ISTIO_ENABLED}" == "true" ]]; then
  bash "${ROOT_DIR}/bin_k8s/09_apply_istio_cb.sh"
fi
kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/mysql.yaml"
kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/kafka.yaml"
kubectl -n msa rollout status deployment/kafka

# 4) MySQL/Kafka 포트포워드 재시작
echo "==> [4/9] MySQL/Kafka 포트포워드 재시작"
kubectl -n msa port-forward svc/mysql 3307:3306 > "${ROOT_DIR}/mysql-port-forward.log" 2>&1 &
kubectl -n msa port-forward svc/kafka 9094:9094 > "${ROOT_DIR}/kafka-port-forward.log" 2>&1 &

# 5) MSA/Consumer 이미지 빌드 및 배포
echo "==> [5/9] MSA/Consumer 이미지 빌드 및 배포"
"${ROOT_DIR}/coupon-service/scripts/deploy_k8s.sh"
"${ROOT_DIR}/point-service/scripts/deploy_k8s.sh"
"${ROOT_DIR}/order-orchestrator/scripts/deploy_k8s.sh"
"${ROOT_DIR}/order-saga-consumer/scripts/deploy_k8s.sh"

# 6) MSA/Consumer 재기동
echo "==> [6/9] MSA/Consumer 재기동"
kubectl -n msa rollout restart deployment/coupon-service
kubectl -n msa rollout restart deployment/point-service
kubectl -n msa rollout restart deployment/order-orchestrator
kubectl -n msa rollout restart deployment/order-saga-consumer
kubectl -n msa rollout status deployment/order-saga-consumer

# 7) 테스트 스냅샷 준비
echo "==> [7/9] 테스트 스냅샷 준비"
"${ROOT_DIR}/bin_common/04_create_test_snapshot_procs.sh"

# 8) MSA 포트포워드
echo "==> [8/9] MSA 포트포워드"
kubectl -n msa port-forward svc/order-orchestrator 8099:8099 > "${ROOT_DIR}/order-port-forward.log" 2>&1 &
kubectl -n msa port-forward svc/coupon-service 8091:8081 > "${ROOT_DIR}/coupon-port-forward.log" 2>&1 &
kubectl -n msa port-forward svc/point-service 8092:8082 > "${ROOT_DIR}/point-port-forward.log" 2>&1 &

# 9) Consumer 로그 출력 (system.out.log)
echo "==> [9/9] Consumer 로그 출력 "
kubectl -n msa logs -f deployment/order-saga-consumer

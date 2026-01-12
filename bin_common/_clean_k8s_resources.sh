#!/usr/bin/env bash
set -euo pipefail

COMMON_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${COMMON_DIR}/lib.sh"

# 1) 포트포워드 종료
echo "==> [1/2] 포트포워드 종료 (8099/8091/8092/3307/9094)"
kill_port 8099
kill_port 8091
kill_port 8092
kill_port 3307
kill_port 9094

# 2) K8s 리소스 삭제 (dev 정리)
echo "==> [2/2] K8s 리소스 삭제"
kubectl -n msa delete -f "${ROOT_DIR}/bin_k8s/order-orchestrator.yaml" --ignore-not-found
kubectl -n msa delete -f "${ROOT_DIR}/bin_k8s/coupon-service.yaml" --ignore-not-found
kubectl -n msa delete -f "${ROOT_DIR}/bin_k8s/point-service.yaml" --ignore-not-found
kubectl -n msa delete -f "${ROOT_DIR}/bin_k8s/kafka.yaml" --ignore-not-found
kubectl delete -f "${ROOT_DIR}/bin_k8s/mysql.yaml" --ignore-not-found

#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "Root Dir " $ROOT_DIR

"${ROOT_DIR}/coupon-service/scripts/deploy_k8s.sh"
"${ROOT_DIR}/point-service/scripts/deploy_k8s.sh"
"${ROOT_DIR}/order-orchestrator/scripts/deploy_k8s.sh"

echo "order-orchestrator rollout 체크 중..."
kubectl -n msa rollout status deployment/order-orchestrator
kubectl port-forward -n msa svc/order-orchestrator 8099:8099  > "${ROOT_DIR}/order-port-forward.log" 2>&1 &
kubectl port-forward -n msa svc/coupon-service 8081:8081  > "${ROOT_DIR}/coupon-port-forward.log" 2>&1 &
kubectl port-forward -n msa svc/point-service 8082:8082  > "${ROOT_DIR}/point-port-forward.log" 2>&1 &

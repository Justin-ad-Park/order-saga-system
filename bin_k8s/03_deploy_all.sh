#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "Root Dir " $ROOT_DIR

"${ROOT_DIR}/coupon-service/scripts/deploy_k8s.sh"
"${ROOT_DIR}/point-service/scripts/deploy_k8s.sh"
"${ROOT_DIR}/order-orchestrator/scripts/deploy_k8s.sh"

echo "order-orchestrator rollout 체크 중..."
kubectl -n msa rollout status deployment/order-orchestrator
kubectl port-forward -n msa svc/order-orchestrator 8099:8099

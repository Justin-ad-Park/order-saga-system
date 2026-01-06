#!/usr/bin/env bash

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "order-orchestrator rollout 체크 중..."
kubectl -n msa rollout status deployment/order-orchestrator
kubectl port-forward -n msa svc/order-orchestrator 8099:8099 > "${ROOT_DIR}/order-port-forward.log" 2>&1 &
kubectl port-forward -n msa svc/coupon-service 8081:8081 > "${ROOT_DIR}/coupon-port-forward.log" 2>&1 &
kubectl port-forward -n msa svc/point-service 8082:8082 > "${ROOT_DIR}/point-port-forward.log" 2>&1 &

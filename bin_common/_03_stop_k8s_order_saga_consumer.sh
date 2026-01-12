#!/usr/bin/env bash
set -euo pipefail

COMMON_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${COMMON_DIR}/lib.sh"

# 1) MSA 포트포워드 종료
echo "==> [1/3] MSA 포트포워드 종료 (8099/8091/8092)"
kill_port 8099
kill_port 8091
kill_port 8092

# 2) Consumer 종료 (Deployment scale down + 기존 Pod 정리)
echo "==> [3/3] Consumer 종료 (replicas=0)"
kubectl -n msa scale deployment/order-saga-consumer --replicas=0 2>/dev/null || true

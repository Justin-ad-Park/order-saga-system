#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${1:-msa}"
DEPLOYMENT="${2:-order-orchestrator}"

# K8s 로그 tail (deployment 기준)
echo "==> logs: ${NAMESPACE}/deployment/${DEPLOYMENT}"
kubectl -n "${NAMESPACE}" logs deployment/"${DEPLOYMENT}" -f

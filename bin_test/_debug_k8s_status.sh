#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${1:-msa}"

# K8s 상태 요약 (pods/svc/deploy/events)
echo "==> namespace: ${NAMESPACE}"
kubectl -n "${NAMESPACE}" get pods -o wide
kubectl -n "${NAMESPACE}" get svc
kubectl -n "${NAMESPACE}" get deploy
kubectl -n "${NAMESPACE}" get events --sort-by=.lastTimestamp

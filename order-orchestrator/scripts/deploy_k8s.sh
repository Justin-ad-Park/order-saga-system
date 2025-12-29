#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SERVICE_DIR="${ROOT_DIR}/order-orchestrator"
MANIFEST="${ROOT_DIR}/bin_k8s/order-orchestrator.yaml"
IMAGE_NAME="order-orchestrator:local"

"${ROOT_DIR}/gradlew" :order-orchestrator:bootJar
docker build -t "${IMAGE_NAME}" "${SERVICE_DIR}"
kubectl apply -f "${MANIFEST}"
kubectl set env -n msa deployment/order-orchestrator SPRING_PROFILES_ACTIVE=dev

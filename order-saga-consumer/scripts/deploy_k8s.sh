#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SERVICE_DIR="${ROOT_DIR}/order-saga-consumer"
MANIFEST="${ROOT_DIR}/bin_k8s/order-saga-consumer.yaml"
IMAGE_NAME="order-saga-consumer:local"

"${ROOT_DIR}/gradlew" :order-saga-consumer:bootJar
docker build -t "${IMAGE_NAME}" "${SERVICE_DIR}"
kubectl apply -f "${MANIFEST}"
kubectl set env -n msa deployment/order-saga-consumer SPRING_PROFILES_ACTIVE=dev

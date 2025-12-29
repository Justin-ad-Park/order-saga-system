#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SERVICE_DIR="${ROOT_DIR}/coupon-service"
MANIFEST="${ROOT_DIR}/bin_k8s/coupon-service.yaml"
IMAGE_NAME="coupon-service:local"

"${ROOT_DIR}/gradlew" :coupon-service:bootJar
docker build -t "${IMAGE_NAME}" "${SERVICE_DIR}"
kubectl apply -f "${MANIFEST}"
kubectl set env -n msa deployment/coupon-service SPRING_PROFILES_ACTIVE=dev

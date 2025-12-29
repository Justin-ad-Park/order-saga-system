#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SERVICE_DIR="${ROOT_DIR}/point-service"
MANIFEST="${ROOT_DIR}/bin_k8s/point-service.yaml"
IMAGE_NAME="point-service:local"

"${ROOT_DIR}/gradlew" :point-service:bootJar
docker build -t "${IMAGE_NAME}" "${SERVICE_DIR}"
kubectl apply -f "${MANIFEST}"
kubectl set env -n msa deployment/point-service SPRING_PROFILES_ACTIVE=dev

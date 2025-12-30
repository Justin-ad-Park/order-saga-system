#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

"${ROOT_DIR}/gradlew" :coupon-service:bootRun \
  -Dspring.profiles.active=test \
  -Dspring.config.name=coupon_application \
  -Dserver.port=8081 &

"${ROOT_DIR}/gradlew" :point-service:bootRun \
  -Dspring.profiles.active=test \
  -Dspring.config.name=point_application \
  -Dserver.port=8082 &

"${ROOT_DIR}/gradlew" :order-orchestrator:bootRun \
  -Dspring.profiles.active=test \
  -Dspring.config.name=orderOS_application \
  -Dserver.port=8080

#!/usr/bin/env bash
set -euo pipefail

DEFAULT_TOPIC="order-saga-events-test"
TOPIC="${1:-${DEFAULT_TOPIC}}"

if ! command -v kcat >/dev/null 2>&1; then
  echo "kcat 이 필요합니다. (예: brew install kcat)"
  exit 1
fi

# Kafka 토픽 메시지 확인 (로컬 포트포워드 기준)
echo "==> Kafka topic: ${TOPIC}"
kcat -b localhost:9094 -t "${TOPIC}" -C -o beginning -e

#!/usr/bin/env bash
set -euo pipefail

COMMON_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${COMMON_DIR}/lib.sh"

# 1) 로컬 MSA 및 Consumer 포트 종료
echo "==> [1/2] 로컬 MSA/Consumer 포트 종료 (8080/8081/8082/8083)"
kill_port 8080
kill_port 8081
kill_port 8082
kill_port 8083

# 2) MySQL/Kafka 포트포워드 종료
echo "==> [2/2] MySQL/Kafka 포트포워드 종료 (3307/9094)"
kill_port 3307
kill_port 9094

#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

kill_port() {
  local port="$1"
  if command -v lsof >/dev/null 2>&1; then
    local pids
    pids="$(lsof -ti tcp:"${port}" || true)"
    if [[ -n "${pids}" ]]; then
      echo "${pids}" | xargs kill
    fi
  else
    ps aux | rg "tcp:${port}" | awk '{print $2}' | xargs kill || true
  fi
}

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

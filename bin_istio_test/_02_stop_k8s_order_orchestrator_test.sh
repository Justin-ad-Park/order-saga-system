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

# 1) MSA 포트포워드 종료
echo "==> [1/3] MSA 포트포워드 종료 (8099/8091/8092)"
kill_port 8099
kill_port 8091
kill_port 8092

# 2) 로컬 Consumer 포트 종료 (k8s-local)
echo "==> [3/3] 로컬 Consumer 포트 종료 (8094)"
kill_port 8094

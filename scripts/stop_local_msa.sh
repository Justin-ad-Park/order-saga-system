#!/usr/bin/env bash
set -euo pipefail

if command -v lsof >/dev/null 2>&1; then
  lsof -ti tcp:8080 | xargs kill
  lsof -ti tcp:8081 | xargs kill
  lsof -ti tcp:8082 | xargs kill
else
  ps aux | rg "gradle.*bootRun" | rg ":order-orchestrator" | awk '{print $2}' | xargs kill
  ps aux | rg "gradle.*bootRun" | rg ":coupon-service" | awk '{print $2}' | xargs kill
  ps aux | rg "gradle.*bootRun" | rg ":point-service" | awk '{print $2}' | xargs kill
fi

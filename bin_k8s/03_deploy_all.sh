#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "Root Dir " $ROOT_DIR

bash "${ROOT_DIR}/bin_k8s/09_apply_istio_cb.sh"

"${ROOT_DIR}/coupon-service/scripts/deploy_k8s.sh"
"${ROOT_DIR}/point-service/scripts/deploy_k8s.sh"
"${ROOT_DIR}/order-orchestrator/scripts/deploy_k8s.sh"

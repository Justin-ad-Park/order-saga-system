#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ISTIO_ENABLED=true exec "${ROOT_DIR}/bin_common/03_prepare_k8s_order_saga_all_k8s.sh"

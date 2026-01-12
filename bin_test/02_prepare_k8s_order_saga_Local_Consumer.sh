#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ISTIO_ENABLED=false exec "${ROOT_DIR}/bin_common/02_prepare_k8s_order_saga_Local_Consumer.sh"

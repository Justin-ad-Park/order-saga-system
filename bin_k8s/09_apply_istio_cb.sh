#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if ! kubectl get ns istio-system >/dev/null 2>&1; then
  echo "Istio not detected; installing..."
  "${ROOT_DIR}/bin_k8s/10_install_istio.sh"
fi

kubectl get ns msa >/dev/null 2>&1 || kubectl create namespace msa
kubectl label namespace msa istio-injection=enabled --overwrite
kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/istio/config/circuit-breaker.yaml"

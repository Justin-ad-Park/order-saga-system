#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ISTIO_VERSION="${ISTIO_VERSION:-1.20.2}"
ISTIO_PROFILE="${ISTIO_PROFILE:-demo}"
ISTIO_DIR="${ROOT_DIR}/bin_k8s/istio/dist/istio-${ISTIO_VERSION}"

if command -v istioctl >/dev/null 2>&1; then
  echo "istioctl already installed: $(command -v istioctl)"
else
  echo "Downloading istioctl ${ISTIO_VERSION}..."
  curl -L https://istio.io/downloadIstio | ISTIO_VERSION="${ISTIO_VERSION}" sh -
  mkdir -p "${ROOT_DIR}/bin_k8s/istio/dist"
  mv "${ROOT_DIR}/istio-${ISTIO_VERSION}" "${ISTIO_DIR}"
  export PATH="${ISTIO_DIR}/bin:${PATH}"
  echo "istioctl installed at ${ISTIO_DIR}/bin/istioctl"
fi

echo "Installing Istio (profile=${ISTIO_PROFILE})..."
istioctl install --set profile="${ISTIO_PROFILE}" -y

echo "Istio CRDs:"
kubectl get crd | rg 'istio|virtualservice|destinationrule' || true

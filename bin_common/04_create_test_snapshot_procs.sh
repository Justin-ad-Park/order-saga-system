#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

kubectl -n msa exec -i deploy/mysql -- \
  mysql -uroot -prootpw < "${ROOT_DIR}/bin_k8s/sql/create_test_snapshots.sql"

echo "Snapshot tables and procedures created."

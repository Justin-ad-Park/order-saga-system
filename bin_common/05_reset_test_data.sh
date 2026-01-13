#!/usr/bin/env bash
set -euo pipefail

kubectl -n msa exec -i deploy/mysql -- \
  mysql -uroot -prootpw -e "CALL coupon_db.sp_reset_coupon_test_data(); CALL point_db.sp_reset_point_test_data();"

echo "Test data reset from snapshots."

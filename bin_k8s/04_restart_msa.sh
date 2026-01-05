#!/usr/bin/env bash
#set -euo pipefail
# bash를 엄격한 모드로 실행하는 옵션 설정
#  -e: 어떤 명령이 실패(비정상 종료)하면 즉시 스크립트 종료
#  -u: 선언되지 않은 변수를 사용하면 에러로 처리
#  -o pipefail: 파이프라인에서 앞 단계가 실패해도 전체 실패로 인식 (기본은 마지막 명령만 체크)

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "${ROOT_DIR}/_03_kill_portforward.sh"

bash "${ROOT_DIR}/_03_kill_portforward.sh"

kubectl -n msa rollout restart deployment/coupon-service
kubectl -n msa rollout restart deployment/point-service
kubectl -n msa rollout restart deployment/order-orchestrator

echo "order-orchestrator rollout 체크 중..."
kubectl -n msa rollout status deployment/order-orchestrator
kubectl port-forward -n msa svc/order-orchestrator 8099:8099  > "${ROOT_DIR}/order-port-forward.log" 2>&1 &

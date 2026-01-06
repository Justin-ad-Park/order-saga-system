#!/usr/bin/env bash
# bash를 엄격한 모드로 실행하는 옵션 설정
#  -e: 어떤 명령이 실패(비정상 종료)하면 즉시 스크립트 종료
#  -u: 선언되지 않은 변수를 사용하면 에러로 처리
#  -o pipefail: 파이프라인에서 앞 단계가 실패해도 전체 실패로 인식 (기본은 마지막 명령만 체크)
set -euo pipefail

# Check if the `lsof` command exists on this system.
if command -v lsof >/dev/null 2>&1; then
  lsof -ti tcp:8099 | xargs kill
  lsof -ti tcp:8091 | xargs kill
  lsof -ti tcp:8092 | xargs kill
else
  ps aux | rg "kubectl port-forward" | rg "8099:8099" | awk '{print $2}' | xargs kill
  ps aux | rg "kubectl port-forward" | rg "8091:8081" | awk '{print $2}' | xargs kill
  ps aux | rg "kubectl port-forward" | rg "8092:8082" | awk '{print $2}' | xargs kill
fi


kubectl -n msa scale deployment/coupon-service --replicas=0
kubectl -n msa scale deployment/point-service --replicas=0
kubectl -n msa scale deployment/order-orchestrator --replicas=0

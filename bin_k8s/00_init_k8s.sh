#!/usr/bin/env bash
# bash를 엄격한 모드로 실행하는 옵션 설정
#  -e: 어떤 명령이 실패(비정상 종료)하면 즉시 스크립트 종료
#  -u: 선언되지 않은 변수를 사용하면 에러로 처리
#  -o pipefail: 파이프라인에서 앞 단계가 실패해도 전체 실패로 인식 (기본은 마지막 명령만 체크)
# set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

kubectl get ns msa >/dev/null 2>&1 || kubectl create namespace msa

bash "${ROOT_DIR}/01_apply_mysql.sh"
bash "${ROOT_DIR}/02_portforward.sh"
bash "${ROOT_DIR}/03_deploy_all.sh"
bash "${ROOT_DIR}/05_msa_portforward.sh"
bash "${ROOT_DIR}/06_deploy_kafka.sh"

kubectl -n msa get pods -o wide

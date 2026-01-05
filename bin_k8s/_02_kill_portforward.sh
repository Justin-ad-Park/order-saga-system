#!/usr/bin/env bash
# bash를 엄격한 모드로 실행하는 옵션 설정
#  -e: 어떤 명령이 실패(비정상 종료)하면 즉시 스크립트 종료
#  -u: 선언되지 않은 변수를 사용하면 에러로 처리
#  -o pipefail: 파이프라인에서 앞 단계가 실패해도 전체 실패로 인식 (기본은 마지막 명령만 체크)
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="${ROOT_DIR}/mysql-port-forward.pid"

if [[ -f "${PID_FILE}" ]]; then
  kill "$(cat "${PID_FILE}")" || true
  rm -f "${PID_FILE}"
fi

# 08. setup_kafka -> main

## 시점
- 2025-12-31

## 비교 기준
- 직전 main 상태: `10270bafd523cc4b6ef6773a7d4f1780f668b79a`
- 브랜치 tip: `6c3739e`

## 주요 변경(커밋 메시지 기반)
- 토픽 생성, 기존 토픽 삭제, 이벤트(토픽) 발행, 브로커 접속 테스트, 이벤트 소비 테스트 추가

## MSA + EDA + SAGA 관점 요약
- K8S/Kafka 배포 및 운영 스크립트

## 연결된 로직 흐름
- 인프라/배포 준비 -> 이벤트 발행/소비 테스트

## 핵심 로직 스니펫(머지 시점 기준)
- `bin_k8s/00_init_k8s.sh`
```bash
#!/usr/bin/env bash
# bash를 엄격한 모드로 실행하는 옵션 설정
#  -e: 어떤 명령이 실패(비정상 종료)하면 즉시 스크립트 종료
#  -u: 선언되지 않은 변수를 사용하면 에러로 처리
#  -o pipefail: 파이프라인에서 앞 단계가 실패해도 전체 실패로 인식 (기본은 마지막 명령만 체크)
# set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

kubectl get ns msa >/dev/null 2>&1 || kubectl create namespace msa

bash "${ROOT_DIR}/09_apply_istio_cb.sh"

bash "${ROOT_DIR}/01_apply_mysql.sh"
bash "${ROOT_DIR}/03_deploy_all.sh"
bash "${ROOT_DIR}/05_msa_portforward.sh"
bash "${ROOT_DIR}/06_deploy_kafka.sh"

kubectl -n msa get pods -o wide
```
- `bin_k8s/01_apply_mysql.sh`
```bash
#!/usr/bin/env bash
# bash를 엄격한 모드로 실행하는 옵션 설정
#  -e: 어떤 명령이 실패(비정상 종료)하면 즉시 스크립트 종료
#  -u: 선언되지 않은 변수를 사용하면 에러로 처리
#  -o pipefail: 파이프라인에서 앞 단계가 실패해도 전체 실패로 인식 (기본은 마지막 명령만 체크)
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

kubectl -n msa apply -f "${ROOT_DIR}/mysql.yaml"
kubectl -n msa get pods
kubectl -n msa get svc
kubectl -n msa get pvc,pv

PID_FILE="${ROOT_DIR}/mysql-port-forward.pid"

if [[ -f "${PID_FILE}" ]]; then
  kill "$(cat "${PID_FILE}")" || true
  rm -f "${PID_FILE}"
fi

kubectl -n msa port-forward svc/mysql 3307:3306 > "${ROOT_DIR}/mysql-port-forward.log" 2>&1 &
echo $! > "${PID_FILE}"
echo "MySQL port-forward started: localhost:3307 -> svc/mysql:3306"
```
- `bin_k8s/02_portforward.sh`
```bash
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

kubectl -n msa port-forward svc/mysql 3307:3306 > "${ROOT_DIR}/mysql-port-forward.log" 2>&1 &
echo $! > "${PID_FILE}"
echo "MySQL port-forward started: localhost:3307 -> svc/mysql:3306"
```
- `bin_k8s/06_deploy_kafka.sh`
```bash
#!/usr/bin/env bash
# bash를 엄격한 모드로 실행하는 옵션 설정
#  -e: 어떤 명령이 실패(비정상 종료)하면 즉시 스크립트 종료
#  -u: 선언되지 않은 변수를 사용하면 에러로 처리
#  -o pipefail: 파이프라인에서 앞 단계가 실패해도 전체 실패로 인식 (기본은 마지막 명령만 체크)
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="${ROOT_DIR}/kafka-port-forward.pid"

kubectl -n msa apply -f "${ROOT_DIR}/kafka.yaml"
kubectl -n msa rollout status deployment/kafka

if [[ -f "${PID_FILE}" ]]; then
  kill "$(cat "${PID_FILE}")" || true
  rm -f "${PID_FILE}"
fi

kubectl -n msa port-forward svc/kafka 9094:9094 > "${ROOT_DIR}/kafka-port-forward.log" 2>&1 &
echo $! > "${PID_FILE}"
echo "Kafka port-forward started: localhost:9094 -> svc/kafka:9094"
```
- `bin_k8s/_00_test_topic.sh` (머지 당시 파일)
```bash
#!/usr/bin/env bash
# bash를 엄격한 모드로 실행하는 옵션 설정
#  -e: 어떤 명령이 실패(비정상 종료)하면 즉시 스크립트 종료
#  -u: 선언되지 않은 변수를 사용하면 에러로 처리
#  -o pipefail: 파이프라인에서 앞 단계가 실패해도 전체 실패로 인식 (기본은 마지막 명령만 체크)
set -euo pipefail

TOPIC="order-events"
BOOTSTRAP_INTERNAL="kafka:9092"
BOOTSTRAP_EXTERNAL="localhost:9094"
TEST_MESSAGE="order-created-1"

echo "== 기존 토픽 삭제 (내부 시스템 토픽 제외) =="
kubectl -n msa exec deploy/kafka -- /bin/bash -lc \
'/opt/kafka/bin/kafka-topics.sh --bootstrap-server '"${BOOTSTRAP_INTERNAL}"' --list | grep -v "^__" | while read -r t; do
  if [[ -n "$t" ]]; then
    echo "삭제: $t"
    /opt/kafka/bin/kafka-topics.sh --bootstrap-server '"${BOOTSTRAP_INTERNAL}"' --delete --topic "$t"
  fi
done'

printf "\n== Kafka 토픽 생성 (K8S 내부) ==\n"
kubectl -n msa exec deploy/kafka -- /bin/bash -lc \
"/opt/kafka/bin/kafka-topics.sh --bootstrap-server ${BOOTSTRAP_INTERNAL} --create --if-not-exists --topic ${TOPIC} --partitions 1 --replication-factor 1"

printf "\n== 브로커 접속 확인 (로컬) ==\n"
kcat -b "${BOOTSTRAP_EXTERNAL}" -L

printf "\n== 이벤트 발행 (로컬) ==\n"
echo "발행 메시지: ${TEST_MESSAGE}"
echo "${TEST_MESSAGE}" | kcat -b "${BOOTSTRAP_EXTERNAL}" -t "${TOPIC}" -P

printf "\n== 이벤트 소비 (로컬) ==\n"
kcat -b "${BOOTSTRAP_EXTERNAL}" -t "${TOPIC}" -C -o beginning -e
```

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

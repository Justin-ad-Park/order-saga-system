# 11. K8s 배포와 실행 스크립트 표준화

## 목표
- 로컬/K8s 실행 흐름을 표준화하는 방법을 이해한다.

## 스토리라인
- 실행 방식이 다양해지며 반복 가능한 스크립트가 필요해짐.

## 관련 커밋
- `f61c6fd`, `ea28648`, `9bc1014`, `248867f`, `1a58beb`, `f8f6a76`

## 커밋 변경 요약
| 커밋 | 주요 변경 요약 | 체크아웃 |
| --- | --- | --- |
| `f61c6fd` | K8s MSA 배포 추가 | `git checkout f61c6fd` |
| `ea28648` | K8s 메시지 테스트 리팩토링 및 bin_k8s 명령어 정리 | `git checkout ea28648` |
| `9bc1014` | 한방에 실행, 종료 스크립트 설명 추가 | `git checkout 9bc1014` |
| `248867f` | Local, K8s 테스트를 위한 쉘 재구성 | `git checkout 248867f` |
| `1a58beb` | bin_test 쉘 추가 정리 및 쉘 설명 추가 | `git checkout 1a58beb` |
| `f8f6a76` | bin_test 쉘 추가 정리 및 쉘 설명 추가 | `git checkout f8f6a76` |

## 핵심 개념
- 배포/실행 분리
- 포트포워딩 표준화

## 기술/기능/프로세스
- 기술: Kubernetes, Docker, kubectl, port-forward
- 기능: 배포/실행/테스트 자동화
- MSA: 다중 서비스 배포 및 운영
- EDA: Kafka 브로커 운영 포함
## 데모/실습
- 로컬 실행: `bin_test/01_prepare_local_order_saga_test.sh`
- K8s 실행: `bin_common/02_prepare_k8s_order_saga_Local_Consumer.sh`

## 코드 발췌 및 설명
- `bin_common/02_prepare_k8s_order_saga_Local_Consumer.sh`: K8s 준비/배포/포트포워드 자동화
```bash
# 3) 네임스페이스/MySQL/Kafka 적용
kubectl get ns msa >/dev/null 2>&1 || kubectl create namespace msa
kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/mysql.yaml"
kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/kafka.yaml"

# 5) MSA 이미지 빌드 및 배포
"${ROOT_DIR}/coupon-service/scripts/deploy_k8s.sh"
"${ROOT_DIR}/point-service/scripts/deploy_k8s.sh"
"${ROOT_DIR}/order-orchestrator/scripts/deploy_k8s.sh"
```
- 왜 필요한가: 배포/실행 자동화의 핵심을 보여줘, 운영 환경에서 반복 가능한 실행 방식을 설명할 수 있다.

## 커밋 상세
### f61c6fd K8s MSA 배포 추가
- 변경 요약: K8s MSA 배포 추가
- 핵심 로직: 배포/실행 자동화 스크립트
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `bin_k8s/01_apply_mysql.sh`, `bin_k8s/02_portforward.sh`
- 코드 발췌: `bin_k8s/01_apply_mysql.sh`
```diff
+kubectl apply -f mysql.yaml
+kubectl get pods -n msa
+kubectl get svc -n msa
+kubectl get pvc,pv -n msa
```
- 코드 발췌: `bin_k8s/02_portforward.sh`
```diff
+kubectl port-forward -n msa svc/mysql 3307:3306 &
+echo $! > port-forward.pid
```

### ea28648 K8s 메시지 테스트 리팩토링 및 bin_k8s 명령어 정리
- 변경 요약: K8s 메시지 테스트 리팩토링 및 bin_k8s 명령어 정리
- 핵심 로직: 테스트 케이스 확장
- 구조 변화: 운영/실행 스크립트 표준화
- 주요 파일: `bin_k8s/00_init_k8s.sh`, `bin_k8s/03_deploy_all.sh`
- 변경 전/후 비교: `bin_k8s/00_init_k8s.sh`
- diff 스타일
```diff
@@ -11,4 +11,7 @@ kubectl get ns msa >/dev/null 2>&1 || kubectl create namespace msa
 
 bash "${ROOT_DIR}/01_apply_mysql.sh"
 bash "${ROOT_DIR}/02_portforward.sh"
+bash "${ROOT_DIR}/03_deploy_all.sh"
 bash "${ROOT_DIR}/06_deploy_kafka.sh"
+
+kubectl -n msa get pods -o wide
```
- 코드 발췌: `bin_k8s/00_init_k8s.sh`
```diff
+bash "${ROOT_DIR}/03_deploy_all.sh"
+
+kubectl -n msa get pods -o wide
```
- 코드 발췌: `bin_k8s/03_deploy_all.sh`
```diff
+kubectl port-forward -n msa svc/order-orchestrator 8099:8099  > "${ROOT_DIR}/order-port-forward.log" 2>&1 &
```

### 9bc1014 한방에 실행, 종료 스크립트 설명 추가
- 변경 요약: 한방에 실행, 종료 스크립트 설명 추가
- 핵심 로직: 핵심 로직 추가/구조 변경
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `bin_k8s/run_k8s.md`
- 변경 전/후 비교: `bin_k8s/run_k8s.md`
kubectl apply -f k8s/mysql.yaml
kubectl get pods -w
kubectl get svc -n msa
kubectl get pvc,pv -n msa

```
Pod가 Running이 되면 OK
```
./bin_k8s/01_apply_mysql.sh

```
Pod가 Running이 되면 OK
```
- diff 스타일
```diff
@@ -21,30 +21,28 @@ mkdir ~/test/mysql
 
 ### 2-2) k8s mysql.yaml 실행
```
-kubectl apply -f k8s/mysql.yaml
-kubectl get pods -w
-kubectl get svc -n msa
-kubectl get pvc,pv -n msa
+./bin_k8s/01_apply_mysql.sh
 
 ```
 Pod가 Running이 되면 OK
 PVC가 Bound인지 확인
 
-### 2-3) 삭제 방법
+### MySQL 중지
```
- 코드 발췌: `bin_k8s/run_k8s.md`
```diff
+./bin_k8s/01_apply_mysql.sh
+### MySQL 중지
+
```
+./bin_k8s/_01_stop_mysql.sh
+### MySQL 삭제 방법
+```
+# 한방에 삭제 
+kubectl delete -f k8s/mysql.yaml 
```

### 248867f Local, K8s 테스트를 위한 쉘 재구성
- 변경 요약: Local, K8s 테스트를 위한 쉘 재구성
- 핵심 로직: 테스트 케이스 확장
- 구조 변화: 운영/실행 스크립트 표준화
- 주요 파일: `bin_test/01_prepare_local_order_orchestrator_test.sh`, `bin_test/02_prepare_k8s_order_orchestrator_test.sh`
- 코드 발췌: `bin_test/01_prepare_local_order_orchestrator_test.sh`
```diff
+#!/usr/bin/env bash
+set -euo pipefail
+
+ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
+
+kill_port() {
+  local port="$1"
+  if command -v lsof >/dev/null 2>&1; then
```
- 코드 발췌: `bin_test/02_prepare_k8s_order_orchestrator_test.sh`
```diff
+#!/usr/bin/env bash
+set -euo pipefail
+
+ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
+
+kill_port() {
+  local port="$1"
+  if command -v lsof >/dev/null 2>&1; then
```

### 1a58beb bin_test 쉘 추가 정리 및 쉘 설명 추가
- 변경 요약: bin_test 쉘 추가 정리 및 쉘 설명 추가
- 핵심 로직: 테스트 케이스 확장
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `bin_test/02_prepare_k8s_order_orchestrator_test.sh`, `coupon-service/scripts/ROOT_DIR_desc.md`
- 변경 전/후 비교: `bin_test/02_prepare_k8s_order_orchestrator_test.sh`
- diff 스타일
```diff
@@ -41,21 +41,10 @@ kubectl -n msa port-forward svc/kafka 9094:9094 > "${ROOT_DIR}/kafka-port-forwar
 
 # 5) MSA 이미지 빌드 및 배포
 echo "==> [5/7] MSA 이미지 빌드 및 배포"
-cd "${ROOT_DIR}"
-"${ROOT_DIR}/gradlew" :coupon-service:bootJar
-docker build -t "coupon-service:local" "${ROOT_DIR}/coupon-service"
-kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/coupon-service.yaml"
-kubectl set env -n msa deployment/coupon-service SPRING_PROFILES_ACTIVE=dev
+"${ROOT_DIR}/coupon-service/scripts/deploy_k8s.sh"
+"${ROOT_DIR}/point-service/scripts/deploy_k8s.sh"
+"${ROOT_DIR}/order-orchestrator/scripts/deploy_k8s.sh"
 
-"${ROOT_DIR}/gradlew" :point-service:bootJar
-docker build -t "point-service:local" "${ROOT_DIR}/point-service"
-kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/point-service.yaml"
```
- 코드 발췌: `bin_test/02_prepare_k8s_order_orchestrator_test.sh`
```diff
+"${ROOT_DIR}/coupon-service/scripts/deploy_k8s.sh"
+"${ROOT_DIR}/point-service/scripts/deploy_k8s.sh"
+"${ROOT_DIR}/order-orchestrator/scripts/deploy_k8s.sh"
```
- 코드 발췌: `coupon-service/scripts/ROOT_DIR_desc.md`
```diff
+# ROOT_DIR 경로 계산 구문 전체 분석
+
+아래 쉘 구문은 현재 실행 중인 스크립트 파일 위치를 기준으로  
+상위 두 단계 디렉터리의 절대 경로를 계산하여 `ROOT_DIR` 변수에 저장한다.
+
+    ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
+
+---
```

### f8f6a76 bin_test 쉘 추가 정리 및 쉘 설명 추가
- 변경 요약: bin_test 쉘 추가 정리 및 쉘 설명 추가
- 핵심 로직: 테스트 케이스 확장
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `coupon-service/scripts/ROOT_DIR_desc.md`, `coupon-service/scripts/deploy_desc.md`
- 코드 발췌: `coupon-service/scripts/ROOT_DIR_desc.md`
```diff
+# ROOT_DIR 경로 계산 구문 전체 분석
+
+아래 쉘 구문은 현재 실행 중인 스크립트 파일 위치를 기준으로  
+상위 두 단계 디렉터리의 절대 경로를 계산하여 `ROOT_DIR` 변수에 저장한다.
+
+    ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
+
+---
```
- 코드 발췌: `coupon-service/scripts/deploy_desc.md`
```diff
+# deploy_k8s.sh 설명 (초보자용)
+
+이 문서는 `deploy_k8s.sh`가 무엇을 하고, 왜 필요한지 초보자도 이해할 수 있도록 풀어서 설명합니다.
+
+
```bash
+#!/usr/bin/env bash
+set -euo pipefail
```

# 07. saga_status -> main

## 시점
- 2025-12-31

## 비교 기준
- 직전 main 상태: `2d3c95786893a0a5b38a91692d0cb1660fbb2572`
- 브랜치 tip: `f4e80f9`

## 주요 변경(커밋 메시지 기반)
- 로컬 및 K8S 테스트용 스크립트 주석 보강

## MSA + EDA + SAGA 관점 요약
- 오케스트레이터 흐름 추가/수정
- K8S/Kafka 배포 및 운영 스크립트

## 연결된 로직 흐름
- 인프라/배포 준비

## 핵심 로직 스니펫(머지 시점 기준)
- `bin_k8s/03_deploy_all.sh`
```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "Root Dir " $ROOT_DIR

"${ROOT_DIR}/coupon-service/scripts/deploy_k8s.sh"
"${ROOT_DIR}/point-service/scripts/deploy_k8s.sh"
"${ROOT_DIR}/order-orchestrator/scripts/deploy_k8s.sh"

echo "order-orchestrator rollout 체크 중..."
kubectl -n msa rollout status deployment/order-orchestrator
kubectl port-forward -n msa svc/order-orchestrator 8099:8099
```
- `bin_k8s/05_restart_msa.sh`
```bash
#!/usr/bin/env bash
# bash를 엄격한 모드로 실행하는 옵션 설정
#  -e: 어떤 명령이 실패(비정상 종료)하면 즉시 스크립트 종료
#  -u: 선언되지 않은 변수를 사용하면 에러로 처리
#  -o pipefail: 파이프라인에서 앞 단계가 실패해도 전체 실패로 인식 (기본은 마지막 명령만 체크)
set -euo pipefail

kubectl -n msa rollout restart deployment/coupon-service
kubectl -n msa rollout restart deployment/point-service
kubectl -n msa rollout restart deployment/order-orchestrator

echo "order-orchestrator rollout 체크 중..."
kubectl -n msa rollout status deployment/order-orchestrator
kubectl port-forward -n msa svc/order-orchestrator 8099:8099
```
- `bin_k8s/_04_portforward_order_orchestrator.sh`
```bash
#!/usr/bin/env bash
# bash를 엄격한 모드로 실행하는 옵션 설정
#  -e: 어떤 명령이 실패(비정상 종료)하면 즉시 스크립트 종료
#  -u: 선언되지 않은 변수를 사용하면 에러로 처리
#  -o pipefail: 파이프라인에서 앞 단계가 실패해도 전체 실패로 인식 (기본은 마지막 명령만 체크)
set -euo pipefail

echo "order-orchestrator rollout 체크 중..."
kubectl -n msa rollout status deployment/order-orchestrator
kubectl port-forward -n msa svc/order-orchestrator 8099:8099
```
- `bin_k8s/_04_stop_msa.sh`
```bash
#!/usr/bin/env bash
# bash를 엄격한 모드로 실행하는 옵션 설정
#  -e: 어떤 명령이 실패(비정상 종료)하면 즉시 스크립트 종료
#  -u: 선언되지 않은 변수를 사용하면 에러로 처리
#  -o pipefail: 파이프라인에서 앞 단계가 실패해도 전체 실패로 인식 (기본은 마지막 명령만 체크)
set -euo pipefail

# Check if the `lsof` command exists on this system.
if command -v lsof >/dev/null 2>&1; then
  lsof -ti tcp:8099 | xargs kill
else
  ps aux | rg "kubectl port-forward" | rg "8099:8099" | awk '{print $2}' | xargs kill
fi


kubectl -n msa scale deployment/coupon-service --replicas=0
kubectl -n msa scale deployment/point-service --replicas=0
kubectl -n msa scale deployment/order-orchestrator --replicas=0
```
- `bin_k8s/__04_kill_portforward.sh`
```bash
#!/usr/bin/env bash
# bash를 엄격한 모드로 실행하는 옵션 설정
#  -e: 어떤 명령이 실패(비정상 종료)하면 즉시 스크립트 종료
#  -u: 선언되지 않은 변수를 사용하면 에러로 처리
#  -o pipefail: 파이프라인에서 앞 단계가 실패해도 전체 실패로 인식 (기본은 마지막 명령만 체크)
set -euo pipefail

# Check if the `lsof` command exists on this system.
if command -v lsof >/dev/null 2>&1; then
  lsof -ti tcp:8099 | xargs kill
else
  ps aux | rg "kubectl port-forward" | rg "8099:8099" | awk '{print $2}' | xargs kill
fi
```
- `order-orchestrator/src/test/httprequest/01_orderOrchestratorK8sTest.http`
```
### 로컬 쿠버네티스에서 MSA 서비스 실행 ###
# bin_k8s/03_deploy_all.sh
## 이미 실행중인데 재기동 (테스트 데이터 초기화)
# bin_k8s/05_restart_msa.sh
#
# 프로세스 종료는
# bin_k8s/_04_stop_msa.sh


### 주문 생성 요청 (K8s Happy Path 예시)
POST http://localhost:8099/api/v1/orders
Content-Type: application/json
Accept: application/json

{
  "couponNumber": "CPN-INT-BOTH-001",
  "pointNumber": "PNT-INT-BOTH-001",
  "paymentNumber": "PAY-001",
  "paymentAmount": 35000,
  "orderItems": [
    {
      "itemNumber": "ITEM-001",
      "quantity": 2
    },
    {
      "itemNumber": "ITEM-002",
      "quantity": 1
    }
  ]
}

###

# 주문 생성 요청 (쿠폰 예약 불가 + 포인트 예약 가능)
POST http://localhost:8099/api/v1/orders
Content-Type: application/json
Accept: application/json

{
  "couponNumber": "CPN-INT-BOTH-RESERVED-001",
  "pointNumber": "PNT-INT-BOTH-AVAILABLE-001",
  "paymentNumber": "PAY-001",
  "paymentAmount": 35000,
  "orderItems": [
    {
      "itemNumber": "ITEM-001",
      "quantity": 2
    },
    {
      "itemNumber": "ITEM-002",
      "quantity": 1
    }
  ]
}

###

# 주문 생성 요청 (쿠폰만)
POST http://localhost:8099/api/v1/orders
Content-Type: application/json
Accept: application/json

{
  "couponNumber": "CPN-INT-ONLY-001",
  "paymentNumber": "PAY-001",
  "paymentAmount": 35000,
  "orderItems": [
    {
      "itemNumber": "ITEM-001",
      "quantity": 2
    },
    {
      "itemNumber": "ITEM-002",
      "quantity": 1
    }
  ]
}

###

# 주문 생성 요청 (포인트만)
POST http://localhost:8099/api/v1/orders
Content-Type: application/json
Accept: application/json

{
  "pointNumber": "PNT-INT-ONLY-001",
  "paymentNumber": "PAY-001",
  "paymentAmount": 35000,
  "orderItems": [
    {
      "itemNumber": "ITEM-001",
      "quantity": 2
    },
    {
      "itemNumber": "ITEM-002",
      "quantity": 1
    }
  ]
}

###

# 주문 생성 요청 (쿠폰/포인트 없음)
POST http://localhost:8099/api/v1/orders
Content-Type: application/json
Accept: application/json

{
  "paymentNumber": "PAY-001",
  "paymentAmount": 35000,
  "orderItems": [
    {
      "itemNumber": "ITEM-001",
      "quantity": 2
    },
    {
      "itemNumber": "ITEM-002",
      "quantity": 1
    }
  ]
}

###

# (선택) 서버 헬스 체크
GET http://localhost:8099/actuator/health
Accept: application/json
```

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

## 커밋 상세
### f61c6fd K8s MSA 배포 추가
- 주요 변경: K8s MSA 배포 추가
- 핵심 코드: `bin_k8s/point-service.yaml`
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: point-service
  namespace: msa
spec:
//--- 생략 ...
```
- 설명: 서비스 구성값을 분리해 환경별 MSA 연동을 명확히 한다.

### ea28648 K8s 메시지 테스트 리팩토링 및 bin_k8s 명령어 정리
- 주요 변경: K8s 메시지 테스트 리팩토링 및 bin_k8s 명령어 정리
- 핵심 코드: `order-orchestrator/src/test/java/com/example/orderorchestrator/adapter/out/kafka/OrderSagaTopicDelete4Dev.java`
```java
class OrderSagaTopicDelete4Dev extends AbstractOrderSagaTopicDelete {
//--- 생략 ...
    protected String topic() {
        return topic;
    }
//--- 생략 ...
}
```
- 설명: Saga 상태 전이를 명시해 흐름의 단계가 코드로 드러나게 한다.

### 9bc1014 한방에 실행, 종료 스크립트 설명 추가
- 주요 변경: 한방에 실행, 종료 스크립트 설명 추가
- 핵심 코드: `bin_k8s/run_k8s.md`
```
//--- 생략 ...

### 2-2) k8s mysql.yaml 실행
```
./bin_k8s/01_apply_mysql.sh

```
Pod가 Running이 되면 OK
PVC가 Bound인지 확인

//--- 생략 ...
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.

### 248867f Local, K8s 테스트를 위한 쉘 재구성
- 주요 변경: Local, K8s 테스트를 위한 쉘 재구성
- 핵심 코드: `bin_test/_debug_k8s_status.sh`
```bash
#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${1:-msa}"

# K8s 상태 요약 (pods/svc/deploy/events)
//--- 생략 ...
```
- 설명: 실행/테스트 스크립트를 통해 분산 시나리오를 재현한다.

### 1a58beb bin_test 쉘 추가 정리 및 쉘 설명 추가
- 주요 변경: bin_test 쉘 추가 정리 및 쉘 설명 추가
- 핵심 코드: `bin_test/02_prepare_k8s_order_orchestrator_test.sh`
```bash
//--- 생략 ...

# 5) MSA 이미지 빌드 및 배포
echo "==> [5/7] MSA 이미지 빌드 및 배포"
"${ROOT_DIR}/coupon-service/scripts/deploy_k8s.sh"
"${ROOT_DIR}/point-service/scripts/deploy_k8s.sh"
"${ROOT_DIR}/order-orchestrator/scripts/deploy_k8s.sh"


# 6) MSA 재기동 및 포트포워드
//--- 생략 ...
```
- 설명: 실행/테스트 스크립트를 통해 분산 시나리오를 재현한다.

### f8f6a76 bin_test 쉘 추가 정리 및 쉘 설명 추가
- 주요 변경: bin_test 쉘 추가 정리 및 쉘 설명 추가
- 핵심 코드: `coupon-service/scripts/deploy_desc.md`
```
# deploy_k8s.sh 설명 (초보자용)

이 문서는 `deploy_k8s.sh`가 무엇을 하고, 왜 필요한지 초보자도 이해할 수 있도록 풀어서 설명합니다.

```bash
#!/usr/bin/env bash
//--- 생략 ...
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.
## 1. 변수 할당
//--- 생략 ...
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.
## 1. 변수 할당
//--- 생략 ...
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.

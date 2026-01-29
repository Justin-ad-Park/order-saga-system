# Chapter 4: Kafka로 이벤트 기반 백본(Backbone) 구축

## 1. 개요: 비동기 통신과 Apache Kafka

이전 챕터에서는 `Outbox Pattern`을 통해 분산 트랜잭션의 데이터 일관성을 확보할 기반을 마련했습니다. 하지만 MSA 간의 직접적인 동기(HTTP) 호출은 여전히 서비스 간의 강한 결합(tight coupling)을 야기하고, 한 서비스의 장애가 다른 서비스로 전파될 위험이 있습니다. 이러한 문제를 해결하고 서비스 간의 느슨한 결합(loose coupling)을 달성하기 위해 **Apache Kafka**를 도입하여 이벤트 기반 아키텍처(Event-Driven Architecture, EDA)의 핵심 인프라를 구축합니다.

본 챕터에서는 Apache Kafka의 기본 개념과 EDA에서의 역할, 그리고 쿠버네티스(Kubernetes) 환경에 Kafka를 배포하고 기본 동작을 확인하는 방법을 학습합니다.

### 핵심 학습 목표
*   Apache Kafka의 기본 개념과 EDA에서 Kafka의 역할을 이해합니다.
*   쿠버네티스 환경에 Kafka 브로커를 배포하는 과정을 학습합니다.
*   Kafka 토픽을 생성하고, 기본적인 프로듀서/컨슈머 동작을 확인하는 방법을 익힙니다.

## 2. Apache Kafka와 이벤트 기반 아키텍처 (EDA)

**Apache Kafka**는 분산 이벤트 스트리밍 플랫폼으로, 높은 처리량과 확장성, 내결함성을 제공하여 이벤트 기반 아키텍처의 핵심 요소로 자리 잡았습니다.

**EDA에서 Kafka의 역할:**
*   **느슨한 결합:** 서비스들은 직접 서로를 호출하는 대신 Kafka를 통해 이벤트를 발행하고 구독합니다. 이로써 서비스 간의 의존성이 줄어들고, 독립적인 개발 및 배포가 가능해집니다.
*   **비동기 통신:** 이벤트는 비동기적으로 처리되므로, 요청을 보낸 서비스는 응답을 기다리지 않고 다음 작업을 진행할 수 있습니다.
*   **내결함성:** Kafka는 메시지를 영구적으로 저장하고, 컨슈머 그룹을 통해 장애 발생 시에도 메시지를 유실하지 않고 처리할 수 있도록 지원합니다.
*   **확장성:** 필요에 따라 Kafka 클러스터를 쉽게 확장하여 처리량을 늘릴 수 있습니다.

**핵심 개념:**
*   **Producer (생산자):** Kafka 토픽으로 메시지(이벤트)를 발행하는 애플리케이션.
*   **Consumer (소비자):** Kafka 토픽에서 메시지(이벤트)를 구독하여 처리하는 애플리케이션.
*   **Topic (토픽):** 이벤트를 카테고리별로 분류하는 논리적인 단위. 프로듀서는 특정 토픽으로 이벤트를 발행하고, 컨슈머는 특정 토픽을 구독합니다.
*   **Broker (브로커):** Kafka 서버를 구성하는 노드. 이벤트를 저장하고 컨슈머에게 전달하는 역할을 합니다.
*   **Zookeeper:** Kafka 클러스터의 메타데이터를 관리하고 브로커의 상태를 조절하는 데 사용됩니다. (최신 Kafka는 Zookeeper 없이 KRaft 모드로도 동작 가능)

## 3. Kafka 구축 관련 Git 이력

Kafka 브로커를 쿠버네티스에 배포하고 테스트 환경을 구성하는 과정과 관련된 주요 Git 커밋입니다.

| 커밋 ID | 날짜 | 주요 변경 요약 |
|---|---|---|
| `499aff6` | 2026-01-04 | Kafka 브로커 구성 및 포트 포워드 스크립트 추가 |
| `10270ba` | 2026-01-04 | 토픽 생성/삭제, 브로커 접속, 이벤트 발행/소비 테스트 추가 |
| `9a613a8` | 2026-01-04 | Kafka 배포 스크립트(`06_deploy_kafka.sh`) 업데이트 |

**(실습 가이드: Git 커밋 확인)**
1.  `git checkout 499aff6` 명령어로 해당 커밋 시점으로 이동하여 `bin_k8s/kafka.yaml` 파일의 초기 내용을 확인해 보세요.
2.  `git diff 499aff6~1 10270ba` 명령어로 Kafka 테스트 스크립트(`bin_k8s/_00_test_topic.sh`)가 추가된 변경사항을 확인할 수 있습니다.

## 4. 핵심 코드 스니펫: Kafka 인프라 구성

### 4.1. 쿠버네티스 `kafka.yaml` (Deployment 및 Service)

Kafka 브로커를 쿠버네티스 클러스터에 배포하기 위한 `Deployment` 및 `Service` 정의입니다. Zookeeper와 함께 StatefulSet으로 관리되는 것이 일반적이지만, 학습 환경에서는 간단한 Deployment로 구성할 수 있습니다.

**`bin_k8s/kafka.yaml`**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: kafka
  namespace: msa # MSA 관련 서비스들이 배포될 네임스페이스
spec:
  ports:
    - port: 9092 # 클라이언트로부터의 내부 통신 포트
      targetPort: 9092
      name: plaintext
    - port: 9094 # 외부 노출용 포트 (Local PC에서 접속할 때 사용)
      targetPort: 9094
      name: external
  selector:
    app: kafka
  type: ClusterIP # 내부에서만 접근 가능한 서비스

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafka
  namespace: msa
spec:
  replicas: 1
  selector:
    matchLabels:
      app: kafka
  template:
    metadata:
      labels:
        app: kafka
    spec:
      hostname: kafka # Kafka 브로커의 호스트명
      containers:
        - name: kafka
          image: confluentinc/cp-kafka:7.6.0 # Confluent Kafka 이미지 사용
          ports:
            - containerPort: 9092
            - containerPort: 9094
          env:
            - name: KAFKA_BROKER_ID
              value: "1"
            - name: KAFKA_ZOOKEEPER_CONNECT
              value: zookeeper.msa.svc.cluster.local:2181 # Zookeeper 서비스 주소
            - name: KAFKA_LISTENER_SECURITY_PROTOCOL_MAP
              value: PLAINTEXT:PLAINTEXT,EXTERNAL:PLAINTEXT
            - name: KAFKA_ADVERTISED_LISTENERS
              value: PLAINTEXT://kafka.msa.svc.cluster.local:9092,EXTERNAL://localhost:9094
            - name: KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR
              value: "1"
            - name: KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR
              value: "1"
            - name: KAFKA_TRANSACTION_STATE_LOG_MIN_ISR
              value: "1"
            - name: KAFKA_CFG_LOG_RETENTION_MS
              value: "30000" # 로그 30초 유지 (개발/테스트용)
---
apiVersion: v1
kind: Service
metadata:
  name: zookeeper
  namespace: msa
spec:
  ports:
    - port: 2181
      targetPort: 2181
      name: client
  selector:
    app: zookeeper
  type: ClusterIP

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: zookeeper
  namespace: msa
spec:
  replicas: 1
  selector:
    matchLabels:
      app: zookeeper
  template:
    metadata:
      labels:
        app: zookeeper
    spec:
      containers:
        - name: zookeeper
          image: confluentinc/cp-zookeeper:7.6.0
          ports:
            - containerPort: 2181
          env:
            - name: ZOOKEEPER_CLIENT_PORT
              value: "2181"
            - name: ZOOKEEPER_TICK_TIME
              value: "2000"
```
**설명:** 위 `kafka.yaml`은 Zookeeper와 Kafka를 각각 `Deployment`와 `Service`로 정의합니다. Kafka는 `confluentinc/cp-kafka` 이미지를 사용하며, `KAFKA_ADVERTISED_LISTENERS`를 통해 클라이언트가 접속할 수 있는 주소(쿠버네티스 내부 `ClusterIP`와 외부 `localhost:9094`)를 설정합니다.

### 4.2. Kafka 배포 스크립트 `06_deploy_kafka.sh`

Kafka 브로커를 쿠버네티스에 배포하고, 외부에서 접속할 수 있도록 포트 포워딩을 설정하는 쉘 스크립트입니다.

**`bin_k8s/06_deploy_kafka.sh`**
```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="${ROOT_DIR}/kafka-port-forward.pid" # 포트 포워딩 PID를 저장할 파일

echo "==> [1/3] Kafka 및 Zookeeper Deployment/Service 적용"
kubectl -n msa apply -f "${ROOT_DIR}/kafka.yaml" # kafka.yaml 파일 적용
kubectl -n msa rollout status deployment/zookeeper # Zookeeper 배포 상태 확인
kubectl -n msa rollout status deployment/kafka # Kafka 배포 상태 확인

echo "==> [2/3] 기존 Kafka 포트 포워딩 프로세스 종료 (있을 경우)"
if [[ -f "${PID_FILE}" ]]; then # PID 파일이 존재하면
  kill "$(cat "${PID_FILE}")" || true # 해당 PID의 프로세스 종료
  rm -f "${PID_FILE}" # PID 파일 삭제
fi

echo "==> [3/3] Kafka 포트 포워딩 시작: localhost:9094 -> svc/kafka:9094"
# 백그라운드에서 포트 포워딩 실행 (로그는 파일로 리다이렉션)
kubectl -n msa port-forward svc/kafka 9094:9094 > "${ROOT_DIR}/kafka-port-forward.log" 2>&1 &
echo $! > "${PID_FILE}" # 백그라운드 프로세스의 PID를 파일에 저장
echo "Kafka port-forward started: localhost:9094 -> svc/kafka:9094"
```
**설명:** 이 스크립트는 `kafka.yaml`을 쿠버네티스에 적용하고, 배포가 완료될 때까지 기다린 후, `localhost:9094`를 `msa` 네임스페이스의 `kafka` 서비스 `9094` 포트로 포워딩하여 로컬 환경에서 Kafka에 접근할 수 있도록 합니다.

## 5. 실습 체크포인트

### 5.1. Kafka 클러스터 배포 및 확인
1.  **`bin_k8s/06_deploy_kafka.sh` 실행:**
    *   프로젝트 루트에서 `./bin_k8s/06_deploy_kafka.sh`를 실행하여 Kafka와 Zookeeper를 쿠버네티스에 배포합니다.
    *   스크립트가 성공적으로 완료되면, Kafka 포트 포워딩이 시작되었다는 메시지를 볼 수 있습니다.
2.  **쿠버네티스 파드 및 서비스 확인:**
    *   `kubectl get pods -n msa` 명령어를 실행하여 `kafka-xxxx`와 `zookeeper-xxxx` 파드가 `Running` 상태인지 확인합니다.
    *   `kubectl get svc -n msa` 명령어를 실행하여 `kafka`와 `zookeeper` 서비스가 생성되었는지 확인합니다.
3.  **Kafka 브로커 연결 테스트:**
    *   **kcat (kafka-cat) 설치:** Kafka 클라이언트 도구인 `kcat`이 설치되어 있지 않다면 먼저 설치합니다. (macOS: `brew install kcat`)
    *   프로젝트 루트에서 `bin_k8s/_90_test_topic.sh` (커밋 `10270ba`에 추가된 스크립트) 스크립트를 실행해 보세요. 이 스크립트는 다음을 수행합니다:
        *   기존 토픽 삭제 (내부 시스템 토픽 제외)
        *   `order-saga-events` 토픽 생성
        *   Kafka 브로커 접속 확인
        *   테스트 메시지 발행
        *   발행된 메시지 소비
    *   **예상 결과:** 스크립트가 성공적으로 실행되면, 테스트 메시지가 발행되고 소비되는 것을 확인할 수 있습니다. 이는 Kafka 클러스터가 정상적으로 작동하고 있음을 의미합니다.

---
`Apache Kafka`를 구축함으로써 우리는 MSA 간의 비동기적이고 느슨하게 결합된 통신 환경을 마련했습니다. 이제 다음 챕터에서는 `order-orchestrator`에서 발생한 Saga 이벤트를 Kafka 토픽으로 발행하는 로직을 구현하는 방법을 알아봅니다.
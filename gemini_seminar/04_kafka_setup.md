# Chapter 4: Kafka로 이벤트 기반 백본(Backbone) 구축하기

지금까지 우리는 Outbox Pattern을 통해 신뢰성 있는 메시지 전달을 위한 '재료'(`OUTBOX_MESSAGE` 테이블)를 준비했습니다. 이제 이 재료를 다른 서비스들에게 전달할 '운송 수단'이 필요합니다. 본 챕터에서는 분산 시스템의 핵심 인프라인 **Apache Kafka**를 도입하고, 우리 시스템의 이벤트 기반 백본으로 구축하는 과정을 살펴봅니다.

## 1. 왜 동기 호출에서 이벤트 기반으로 전환하는가?

`order-orchestrator`가 `coupon-service`와 `point-service`를 직접 호출(HTTP 동기 호출)하는 방식은 구현이 간단하지만, 시스템이 커질수록 다음과 같은 한계에 부딪힙니다.

*   **강한 결합(Tight Coupling):** `order-orchestrator`는 다른 서비스들의 위치(IP, 도메인)와 API 명세를 모두 알고 있어야 합니다.
*   **낮은 유연성 및 확장성:** 새로운 서비스(예: 재고 서비스)가 주문 프로세스에 참여하려면 `order-orchestrator`의 코드를 계속해서 수정해야 합니다.
*   **연쇄 장애(Cascading Failures):** `point-service` 하나의 장애가 `order-orchestrator`까지 전파되어 전체 주문 프로세스가 중단될 수 있습니다.

이러한 문제를 해결하기 위해, 우리는 서비스 간의 상호작용을 비동기적인 **이벤트(Event)** 기반으로 전환하기로 결정했습니다. 이 구조는 서비스 간의 결합도를 낮추고(느슨한 결합), 유연성과 확장성을 크게 향상시킵니다.

## 2. 주요 Git 이력

아래 커밋들은 Kafka 인프라를 구성하고, 이벤트 발행 및 소비를 테스트하는 기반을 마련하는 과정을 보여줍니다.
```
* aeceecc | 2026-01-05 | 테스트 토픽 분리 & 토픽 발행 테스트 OrderSagaEventPublishIntegrationTest
* 9a613a8 | 2025-12-31 | 토픽 생성, 기존 토픽 삭제, 이벤트(토픽) 발행, 브로커 접속 테스트, 이벤트 소비 테스트 추가
* 499aff6 | 2025-12-31 | Kafka 브로커 구성 및 포트 포워드
```

## 3. 핵심 코드 스니펫

### 쿠버네티스 기반 Kafka 배포 설정

`499aff6` 커밋에서 추가된 `kafka.yaml` 파일은 쿠버네티스 환경에 Kafka 브로커를 배포하기 위한 모든 정의를 담고 있습니다. 개발자는 이 파일을 `kubectl apply` 명령어로 실행하기만 하면 복잡한 Kafka 클러스터를 손쉽게 구축할 수 있습니다.

**`bin_k8s/kafka.yaml`**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: kafka
  namespace: msa
spec:
  selector:
    app: kafka
  ports:
    - name: broker
      port: 9092
      targetPort: 9092
# ...
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
      containers:
        - name: kafka
          image: apache/kafka:3.7.0
          ports:
            - containerPort: 9092
            - containerPort: 9093
            - containerPort: 9094
          env:
            - name: KAFKA_CLUSTER_ID
              value: "fhqbRm3XSFiwYlvFP9MIyA"
          command:
            - /bin/bash
            - -lc
            - |
              # Kafka 서버 설정 (kraft 모드)
              set -e
              cat > /tmp/kraft.properties <<'EOF'
              process.roles=broker,controller
              node.id=1
              controller.quorum.voters=1@kafka:9093
              listeners=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093,EXTERNAL://0.0.0.0:9094
              advertised.listeners=PLAINTEXT://kafka:9092,EXTERNAL://localhost:9094
              # ...
              EOF
              # 스토리지 포맷 후 서버 실행
              /opt/kafka/bin/kafka-storage.sh format -t "${KAFKA_CLUSTER_ID}" -c /tmp/kraft.properties --ignore-formatted
              exec /opt/kafka/bin/kafka-server-start.sh /tmp/kraft.properties
# ...
```
이처럼 인프라 구성을 코드로 관리(Infrastructure as Code, IaC)함으로써, 개발 환경과 운영 환경의 일관성을 유지하고 누구나 동일한 인프라를 쉽게 구축할 수 있게 됩니다.

---
이제 우리 시스템에는 서비스들의 이벤트를 실어 나를 수 있는 튼튼한 '버스'인 Kafka가 마련되었습니다. 다음 챕터에서는 `order-orchestrator`가 Outbox Pattern을 활용하여 어떻게 이 버스에 '승객'(이벤트)을 태우는지, 즉 이벤트를 발행하는 로직을 구체적으로 살펴보겠습니다.
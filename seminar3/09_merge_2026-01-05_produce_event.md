# 09. produce_event -> main

## 시점
- 2026-01-05

## 비교 기준
- 직전 main 상태: `ea2864873ba18a2a5b92faecbb8a1beb5889d96c`
- 브랜치 tip: `9bc1014`

## 주요 변경(커밋 메시지 기반)
- 한방에 실행, 종료 스크립트 설명 추가

## MSA + EDA + SAGA 관점 요약
- K8S/Kafka 배포 및 운영 스크립트
- 이벤트 발행/소비 확인 스크립트 활용
- OrderSagaEvent Kafka 발행 어댑터 추가

## 연결된 로직 흐름
- 인프라/배포 준비 -> 이벤트 발행 -> 이벤트 발행/소비 테스트

## 핵심 로직 스니펫(머지 시점 기준)
- `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/kafka/OrderSagaEventKafkaPublisher.java`
```java
package com.example.orderorchestrator.adapter.out.kafka;

import com.example.orderorchestrator.application.port.out.OrderSagaEventPublisher;
import com.example.orderorchestrator.domain.event.OrderSagaEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderSagaEventKafkaPublisher implements OrderSagaEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(OrderSagaEventKafkaPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public OrderSagaEventKafkaPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${order.saga.events.topic:order-saga-events}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publish(OrderSagaEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, event.orderId(), payload);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize OrderSagaEvent: orderId={}", event.orderId(), ex);
        }
    }
}
```
- `order-orchestrator/src/main/java/com/example/orderorchestrator/application/port/out/OrderSagaEventPublisher.java`
```java
package com.example.orderorchestrator.application.port.out;

import com.example.orderorchestrator.domain.event.OrderSagaEvent;

public interface OrderSagaEventPublisher {
    void publish(OrderSagaEvent event);
}
```
- `order-orchestrator/src/main/java/com/example/orderorchestrator/domain/event/OrderSagaEvent.java`
```java
package com.example.orderorchestrator.domain.event;

import com.example.orderorchestrator.domain.model.status.OrderSagaStatus;

public record OrderSagaEvent(
        String orderId,
        String sagaId,
        OrderSagaEventType type,
        OrderSagaStatus status
) {
}
```
- `bin_k8s/run_k8s.md`
~~~text
# 쿠버네티스 설정 및 mysql 설치
```
kubectl create namespace msa
kubectl get ns

mkdir ~/test/mysql
```

### 아래 모든 과정은 .sh로 만들어 둠

## Step 2. K8s에 MySQL 배포 (Local Path PV + PVC + Deployment + Service)

목표: ~/test/mysql을 PV로 연결 → PVC로 바인딩 → MySQL에 마운트

### 2-1) PV/PVC/Service/Deployment 매니페스트 작성

아래를 k8s/mysql.yaml로 저장(예시).

⚠️ 주의: 로컬 K8s 종류에 따라 hostPath로 ~/test/mysql를 바로 쓰는 게 안 되는 경우가 있어.
그럴 때는 “Docker Desktop의 HostPath” 지원 범위 또는 “local-path-provisioner” 방식으로 바꿔야 해.

### 2-2) k8s mysql.yaml 실행
```
./bin_k8s/01_apply_mysql.sh

```
Pod가 Running이 되면 OK
PVC가 Bound인지 확인

### MySQL 중지
```
./bin_k8s/_01_stop_mysql.sh
```

### MySQL 삭제 방법
```
# 한방에 삭제 
kubectl delete -f k8s/mysql.yaml 
# 또는
./bin_k8s/_03_delete_all_deploy_msa.sh
```

## 3. 쿠버네티스의 mysql에 접속하기 위한 포트 포워드 실행 
```
./bin_k8s/02_portforward.sh
```

### 3-1) mySql에 접속해서 스키마(데이터베이스)와 계정 생성
URL: jdbc:mysql://localhost:3307
user: root
pwd: rootpw


```mysql
-- Order Orchestrator
CREATE DATABASE IF NOT EXISTS order_orchestrator_db;
CREATE USER IF NOT EXISTS 'order_orchestrator_user'@'%' IDENTIFIED BY 'order_orchestrator_pw';
GRANT ALL PRIVILEGES ON order_orchestrator_db.* TO 'order_orchestrator_user'@'%';

-- Coupon Service
CREATE DATABASE IF NOT EXISTS coupon_db;
CREATE USER IF NOT EXISTS 'coupon_user'@'%' IDENTIFIED BY 'coupon_pw';
GRANT ALL PRIVILEGES ON coupon_db.* TO 'coupon_user'@'%';

-- Point Service
CREATE DATABASE IF NOT EXISTS point_db;
CREATE USER IF NOT EXISTS 'point_user'@'%' IDENTIFIED BY 'point_pw';
GRANT ALL PRIVILEGES ON point_db.* TO 'point_user'@'%';


FLUSH PRIVILEGES;
```


```java
/* 컬럼 순서 강제 조정 */
ALTER TABLE outbox_message
MODIFY COLUMN saga_status VARCHAR(255) NOT NULL AFTER payload;

ALTER TABLE outbox_message
MODIFY COLUMN order_status VARCHAR(255) NOT NULL AFTER payload;

ALTER TABLE outbox_message
MODIFY COLUMN point_status VARCHAR(255) NOT NULL AFTER payload;

ALTER TABLE outbox_message
MODIFY COLUMN coupon_status VARCHAR(255) NOT NULL AFTER payload;

```

## MSA K8s에 배포 및 실행
```
./bin_k8s/03_deploy_all.sh


## 종료 
./bin_k8s/_03_stop_msa.sh

## 삭제 
./bin_k8s/_03_delete_all_deploy_msa.sh

```

### 수정 사항 재배포 
```
./bin_k8s/04_restart_msa.sh
```

## 카프카 배포 및 로컬 포트포워드
```
./bin_k8s/06_deploy_kafka.sh

## 카프카 포트포워드 종료 및 리소스 삭제
./bin_k8s/_06_kill_kafka.sh
```
~~~
- `bin_k8s/_00_test_topic.sh` (머지 시점 기준)
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

# 08. Kafka 기반 EDA 구성

## 목표
- 토픽 생성, 이벤트 발행/소비, 테스트 구조를 이해한다.

## 스토리라인
- 사가를 안정적으로 연결하기 위해 이벤트 흐름을 검증.

## 관련 커밋
- `499aff6`, `10270ba`, `9a613a8`, `aeceecc`, `9aa633c`

## 커밋 변경 요약
| 커밋 | 주요 변경 요약 | 체크아웃 |
| --- | --- | --- |
| `499aff6` | Kafka 브로커 구성 및 포트 포워드 | `git checkout 499aff6` |
| `10270ba` | 토픽 생성, 기존 토픽 삭제, 이벤트(토픽) 발행, 브로커 접속 테스트, 이벤트 소비 테스트 추가 | `git checkout 10270ba` |
| `9a613a8` | 토픽 생성, 기존 토픽 삭제, 이벤트(토픽) 발행, 브로커 접속 테스트, 이벤트 소비 테스트 추가 | `git checkout 9a613a8` |
| `aeceecc` | 테스트 토픽 분리 & 토픽 발행 테스트 OrderSagaEventPublishIntegrationTest | `git checkout aeceecc` |
| `9aa633c` | 통합 테스트 카프카 토픽 로그 추가 | `git checkout 9aa633c` |

## 핵심 개념
- 토픽 관리, 테스트 환경 분리
- 이벤트 발행 테스트

## 기술/기능/프로세스
- 기술: Kafka, 토픽 관리, 이벤트 발행/소비 테스트
- 기능: 토픽 생성/삭제, 발행/소비 검증
- MSA: 서비스 간 비동기 연결
- EDA: 이벤트 토픽 분리와 테스트 전략
## 데모/실습
- 카프카 테스트 코드: `order-orchestrator/src/test/java/.../adapter/out/kafka/*`

## 코드 발췌 및 설명
- `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/kafka/OrderSagaTopicConfig.java`: 테스트 프로파일에서 토픽 설정/초기화
```java
    @Bean
    public KafkaAdmin.NewTopics orderSagaEventsTopic(
            @Value("${order.saga.events.topic:order-saga-events}") String topic
    ) {
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name(topic)
                        .config(TopicConfig.RETENTION_MS_CONFIG, "30000")
                        .build()
        );
    }

    @Bean
    public ApplicationRunner recreateTestTopicWithConfig(
            KafkaAdmin kafkaAdmin,
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${order.saga.events.topic:order-saga-events}") String topic
    ) {
        return args -> {
            kafkaAdmin.initialize();
        };
    }
```
- 왜 필요한가: 테스트 토픽 구성과 초기화가 어디서 이뤄지는지 보여줘, EDA 검증 방법을 설명할 수 있다.

## 커밋 상세
### 499aff6 Kafka 브로커 구성 및 포트 포워드
- 변경 요약: Kafka 브로커 구성 및 포트 포워드
- 핵심 로직: Kafka 토픽/이벤트 설정
- 구조 변화: 운영/실행 스크립트 표준화
- 주요 파일: `bin_k8s/06_deploy_kafka.sh`, `bin_k8s/_06_kill_kafka.sh`
- 코드 발췌: `bin_k8s/06_deploy_kafka.sh`
```diff
+#!/usr/bin/env bash
+# bash를 엄격한 모드로 실행하는 옵션 설정
+#  -e: 어떤 명령이 실패(비정상 종료)하면 즉시 스크립트 종료
+#  -u: 선언되지 않은 변수를 사용하면 에러로 처리
+#  -o pipefail: 파이프라인에서 앞 단계가 실패해도 전체 실패로 인식 (기본은 마지막 명령만 체크)
+set -euo pipefail
+
+ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
```
- 코드 발췌: `bin_k8s/_06_kill_kafka.sh`
```diff
+#!/usr/bin/env bash
+# bash를 엄격한 모드로 실행하는 옵션 설정
+#  -e: 어떤 명령이 실패(비정상 종료)하면 즉시 스크립트 종료
+#  -u: 선언되지 않은 변수를 사용하면 에러로 처리
+#  -o pipefail: 파이프라인에서 앞 단계가 실패해도 전체 실패로 인식 (기본은 마지막 명령만 체크)
+set -euo pipefail
+
+ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
```

### 10270ba 토픽 생성, 기존 토픽 삭제, 이벤트(토픽) 발행, 브로커 접속 테스트, 이벤트 소비 테스트 추가
- 변경 요약: 토픽 생성, 기존 토픽 삭제, 이벤트(토픽) 발행, 브로커 접속 테스트, 이벤트 소비 테스트 추가
- 핵심 로직: Kafka 토픽/이벤트 설정
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `bin_k8s/06_deploy_kafka.sh`
- 코드 발췌: `bin_k8s/06_deploy_kafka.sh`
```diff
+PID_FILE="${ROOT_DIR}/kafka-port-forward.pid"
+if [[ -f "${PID_FILE}" ]]; then
+  kill "$(cat "${PID_FILE}")" || true
+  rm -f "${PID_FILE}"
+fi
+
+echo $! > "${PID_FILE}"
```

### 9a613a8 토픽 생성, 기존 토픽 삭제, 이벤트(토픽) 발행, 브로커 접속 테스트, 이벤트 소비 테스트 추가
- 변경 요약: 토픽 생성, 기존 토픽 삭제, 이벤트(토픽) 발행, 브로커 접속 테스트, 이벤트 소비 테스트 추가
- 핵심 로직: Kafka 토픽/이벤트 설정
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `bin_k8s/06_deploy_kafka.sh`
- 코드 발췌: `bin_k8s/06_deploy_kafka.sh`
```diff
+PID_FILE="${ROOT_DIR}/kafka-port-forward.pid"
+if [[ -f "${PID_FILE}" ]]; then
+  kill "$(cat "${PID_FILE}")" || true
+  rm -f "${PID_FILE}"
+fi
+
+echo $! > "${PID_FILE}"
```

### aeceecc 테스트 토픽 분리 & 토픽 발행 테스트 OrderSagaEventPublishIntegrationTest
- 변경 요약: 테스트 토픽 분리 & 토픽 발행 테스트 OrderSagaEventPublishIntegrationTest
- 핵심 로직: Kafka 토픽/이벤트 설정
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/kafka/OrderSagaEventKafkaPublisher.java`, `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/kafka/OrderSagaTopicConfig.java`
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/kafka/OrderSagaEventKafkaPublisher.java`
```diff
+package com.example.orderorchestrator.adapter.out.kafka;
+
+import com.example.orderorchestrator.application.port.out.OrderSagaEventPublisher;
+import com.example.orderorchestrator.domain.event.OrderSagaEvent;
+import com.fasterxml.jackson.core.JsonProcessingException;
+import com.fasterxml.jackson.databind.ObjectMapper;
+import org.slf4j.Logger;
+import org.slf4j.LoggerFactory;
```
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/kafka/OrderSagaTopicConfig.java`
```diff
+package com.example.orderorchestrator.adapter.out.kafka;
+
+import org.apache.kafka.common.config.TopicConfig;
+import org.springframework.beans.factory.annotation.Value;
+import org.springframework.context.annotation.Bean;
+import org.springframework.context.annotation.Configuration;
+import org.springframework.context.annotation.Profile;
+import org.springframework.kafka.config.TopicBuilder;
```

### 9aa633c 통합 테스트 카프카 토픽 로그 추가
- 변경 요약: 통합 테스트 카프카 토픽 로그 추가
- 핵심 로직: Kafka 토픽/이벤트 설정
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `order-orchestrator/src/test/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationIntegrationTest.java`
- 코드 발췌: `order-orchestrator/src/test/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationIntegrationTest.java`
```diff
+import org.apache.kafka.clients.admin.AdminClient;
+import org.apache.kafka.clients.admin.AdminClientConfig;
+import org.apache.kafka.clients.consumer.ConsumerConfig;
+import org.apache.kafka.clients.consumer.KafkaConsumer;
+import org.apache.kafka.common.serialization.StringDeserializer;
+import org.junit.jupiter.api.TestInstance;
+import org.springframework.beans.factory.annotation.Value;
+import java.util.Set;
```

### 499aff6 Kafka 브로커 구성 및 포트 포워드
- 변경 요약: Kafka 브로커 구성 및 포트 포워드
- 핵심 로직: Kafka 토픽/이벤트 설정
- 구조 변화: 운영/실행 스크립트 표준화
- 주요 파일: `bin_k8s/06_deploy_kafka.sh`, `bin_k8s/_06_kill_kafka.sh`
- 코드 발췌: `bin_k8s/06_deploy_kafka.sh`
```diff
+#!/usr/bin/env bash
+# bash를 엄격한 모드로 실행하는 옵션 설정
+#  -e: 어떤 명령이 실패(비정상 종료)하면 즉시 스크립트 종료
+#  -u: 선언되지 않은 변수를 사용하면 에러로 처리
+#  -o pipefail: 파이프라인에서 앞 단계가 실패해도 전체 실패로 인식 (기본은 마지막 명령만 체크)
+set -euo pipefail
+
+ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
```
- 코드 발췌: `bin_k8s/_06_kill_kafka.sh`
```diff
+#!/usr/bin/env bash
+# bash를 엄격한 모드로 실행하는 옵션 설정
+#  -e: 어떤 명령이 실패(비정상 종료)하면 즉시 스크립트 종료
+#  -u: 선언되지 않은 변수를 사용하면 에러로 처리
+#  -o pipefail: 파이프라인에서 앞 단계가 실패해도 전체 실패로 인식 (기본은 마지막 명령만 체크)
+set -euo pipefail
+
+ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
```

### 10270ba 토픽 생성, 기존 토픽 삭제, 이벤트(토픽) 발행, 브로커 접속 테스트, 이벤트 소비 테스트 추가
- 변경 요약: 토픽 생성, 기존 토픽 삭제, 이벤트(토픽) 발행, 브로커 접속 테스트, 이벤트 소비 테스트 추가
- 핵심 로직: Kafka 토픽/이벤트 설정
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `bin_k8s/06_deploy_kafka.sh`
- 코드 발췌: `bin_k8s/06_deploy_kafka.sh`
```diff
+PID_FILE="${ROOT_DIR}/kafka-port-forward.pid"
+if [[ -f "${PID_FILE}" ]]; then
+  kill "$(cat "${PID_FILE}")" || true
+  rm -f "${PID_FILE}"
+fi
+
+echo $! > "${PID_FILE}"
```

### 9a613a8 토픽 생성, 기존 토픽 삭제, 이벤트(토픽) 발행, 브로커 접속 테스트, 이벤트 소비 테스트 추가
- 변경 요약: 토픽 생성, 기존 토픽 삭제, 이벤트(토픽) 발행, 브로커 접속 테스트, 이벤트 소비 테스트 추가
- 핵심 로직: Kafka 토픽/이벤트 설정
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `bin_k8s/06_deploy_kafka.sh`
- 코드 발췌: `bin_k8s/06_deploy_kafka.sh`
```diff
+PID_FILE="${ROOT_DIR}/kafka-port-forward.pid"
+if [[ -f "${PID_FILE}" ]]; then
+  kill "$(cat "${PID_FILE}")" || true
+  rm -f "${PID_FILE}"
+fi
+
+echo $! > "${PID_FILE}"
```

### aeceecc 테스트 토픽 분리 & 토픽 발행 테스트 OrderSagaEventPublishIntegrationTest
- 변경 요약: 테스트 토픽 분리 & 토픽 발행 테스트 OrderSagaEventPublishIntegrationTest
- 핵심 로직: Kafka 토픽/이벤트 설정
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/kafka/OrderSagaEventKafkaPublisher.java`, `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/kafka/OrderSagaTopicConfig.java`
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/kafka/OrderSagaEventKafkaPublisher.java`
```diff
+package com.example.orderorchestrator.adapter.out.kafka;
+
+import com.example.orderorchestrator.application.port.out.OrderSagaEventPublisher;
+import com.example.orderorchestrator.domain.event.OrderSagaEvent;
+import com.fasterxml.jackson.core.JsonProcessingException;
+import com.fasterxml.jackson.databind.ObjectMapper;
+import org.slf4j.Logger;
+import org.slf4j.LoggerFactory;
```
- 코드 발췌: `order-orchestrator/src/main/java/com/example/orderorchestrator/adapter/out/kafka/OrderSagaTopicConfig.java`
```diff
+package com.example.orderorchestrator.adapter.out.kafka;
+
+import org.apache.kafka.common.config.TopicConfig;
+import org.springframework.beans.factory.annotation.Value;
+import org.springframework.context.annotation.Bean;
+import org.springframework.context.annotation.Configuration;
+import org.springframework.context.annotation.Profile;
+import org.springframework.kafka.config.TopicBuilder;
```

### 9aa633c 통합 테스트 카프카 토픽 로그 추가
- 변경 요약: 통합 테스트 카프카 토픽 로그 추가
- 핵심 로직: Kafka 토픽/이벤트 설정
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `order-orchestrator/src/test/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationIntegrationTest.java`
- 코드 발췌: `order-orchestrator/src/test/java/com/example/orderorchestrator/adapter/in/web/OrderOrchestrationIntegrationTest.java`
```diff
+import org.apache.kafka.clients.admin.AdminClient;
+import org.apache.kafka.clients.admin.AdminClientConfig;
+import org.apache.kafka.clients.consumer.ConsumerConfig;
+import org.apache.kafka.clients.consumer.KafkaConsumer;
+import org.apache.kafka.common.serialization.StringDeserializer;
+import org.junit.jupiter.api.TestInstance;
+import org.springframework.beans.factory.annotation.Value;
+import java.util.Set;
```

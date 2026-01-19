# 09. Saga Consumer 구현과 보상 호출

## 목표
- 소비자에서 이벤트를 처리하고 confirm/compensate를 수행하는 흐름을 이해한다.

## 스토리라인
- 오케스트레이터 이벤트를 소비하여 실제 MSA 상태를 확정/보상.

## 관련 커밋
- `3afbfb9`, `0b73be2`, `a1f74d8`, `576a868`, `5a250f8`, `9e08ba1`

## 커밋 변경 요약
| 커밋 | 주요 변경 요약 | 체크아웃 |
| --- | --- | --- |
| `3afbfb9` | Comsumer 기본 프로젝트 및 기본 로직 구성 | `git checkout 3afbfb9` |
| `0b73be2` | ### Saga 컨슈머 confirm, compensate 로직 추가 ### | `git checkout 0b73be2` |
| `a1f74d8` | ### Consumer host Test ### | `git checkout a1f74d8` |
| `576a868` | Comsumer 실행 시 profile 설정 안되는 오류 수정 | `git checkout 576a868` |
| `5a250f8` | ### Saga Local & K8s + Host Consumer 테스트 완료 ### | `git checkout 5a250f8` |
| `9e08ba1` | ### Consumer K8s 배포 및 실행 스크립트 추사 ### | `git checkout 9e08ba1` |

## 핵심 개념
- 소비자 책임(메시지 처리, 상태 갱신)
- 로컬/호스트/K8s 실행 분리

## 기술/기능/프로세스
- 기술: Spring Kafka Consumer, WebClient
- 기능: 이벤트 처리, confirm/compensate 호출
- MSA: 소비자 역할 분리
- EDA: order-saga-events 소비
## 데모/실습
- 소비자 실행: `bin_k8s/07_run_local_consumer.sh`, `bin_k8s/07_run_consumer_host2K8s.sh`

## 커밋 상세
### 3afbfb9 Comsumer 기본 프로젝트 및 기본 로직 구성
- 주요 변경: Comsumer 기본 프로젝트 및 기본 로직 구성
- 핵심 코드: `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/adapter/in/kafka/OrderSagaEventConsumer.java`
```java
public class OrderSagaEventConsumer {
//--- 생략 ...
    public void consume(List<ConsumerRecord<String, String>> records) {
        records.forEach(record -> System.out.println(
                "### Kafka payloads ### : " + record.topic()
                        + " partition=" + record.partition()
                        + " offset=" + record.offset()
                        + " key=" + record.key()
                        + " value=" + record.value()
        ));
    }
//--- 생략 ...
}
```
- 설명: Kafka 컨슈머가 이벤트를 수신해 confirm/compensate 흐름을 이어간다.

### 0b73be2 ### Saga 컨슈머 confirm, compensate 로직 추가 ###
- 주요 변경: ### Saga 컨슈머 confirm, compensate 로직 추가 ###
- 핵심 코드: `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/application/service/ProcessOrderSagaEventService.java`
```java
public class ProcessOrderSagaEventService implements ProcessOrderSagaEventUseCase {
//--- 생략 ...
    public void process(String orderId, String status) {
        if (orderId == null || orderId.isBlank()) {
            System.out.println("### OrderSaga lookup skipped ### : empty orderId");
            return;
        }

        OrderSagaInfo info = loadOrderSagaPort.findByOrderId(orderId)
                .orElse(null);

        if (info == null) {
            System.out.println("### OrderSaga not found ### : orderId=" + orderId
                    + " status=" + status);
            return;
        }

        System.out.println("### OrderSaga details ### : orderId=" + orderId
                + " status=" + status
                + " couponNumber=" + info.couponNumber()
                + " pointNumber=" + info.pointNumber());

        OrderSagaStatus sagaStatus = parseSagaStatus(status);
        if (sagaStatus == null) {
            System.out.println("### OrderSaga status skipped ### : unsupported status=" + status);
            return;
        }

        if (sagaStatus == OrderSagaStatus.Reserved) {
            handleConfirm(orderId, info);
            return;
        }

        if (sagaStatus == OrderSagaStatus.Compensating) {
            handleCompensate(orderId, info);
        }
    }
//--- 생략 ...
}
```
- 설명: Saga 상태에 따라 confirm/compensate를 분기해 보상 흐름을 완성한다.

### a1f74d8 ### Consumer host Test ###
- 주요 변경: ### Consumer host Test ###
- 핵심 코드: `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/OrderSagaConsumerApplication.java`
```java
public class OrderSagaConsumerApplication {
//--- 생략 ...
        new SpringApplicationBuilder(OrderSagaConsumerApplication.class)
                .properties(
                        "spring.config.name=OSC_application"
                ).run(args);
    }
}
//--- 생략 ...
}
```
- 설명: Kafka 컨슈머가 이벤트를 수신해 confirm/compensate 흐름을 이어간다.

### 576a868 Comsumer 실행 시 profile 설정 안되는 오류 수정
- 주요 변경: Comsumer 실행 시 profile 설정 안되는 오류 수정
- 핵심 코드: `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/OrderSagaConsumerApplication.java`
```java
public class OrderSagaConsumerApplication {
//--- 생략 ...
        new SpringApplicationBuilder(OrderSagaConsumerApplication.class)
                .properties(
                        "spring.config.name=OSC_application"
                ).run(args);
    }
}
//--- 생략 ...
}
```
- 설명: Kafka 컨슈머가 이벤트를 수신해 confirm/compensate 흐름을 이어간다.

### 5a250f8 ### Saga Local & K8s + Host Consumer 테스트 완료 ###
- 주요 변경: ### Saga Local & K8s + Host Consumer 테스트 완료 ###
- 핵심 코드: `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/OrderSagaConsumerApplication.java`
```java
public class OrderSagaConsumerApplication {
//--- 생략 ...
        new SpringApplicationBuilder(OrderSagaConsumerApplication.class)
                .properties(
                        "spring.config.name=OSC_application"
                ).run(args);
    }
}
//--- 생략 ...
}
```
- 설명: Kafka 컨슈머가 이벤트를 수신해 confirm/compensate 흐름을 이어간다.

### 9e08ba1 ### Consumer K8s 배포 및 실행 스크립트 추사 ###
- 주요 변경: ### Consumer K8s 배포 및 실행 스크립트 추사 ###
- 핵심 코드: `order-saga-consumer/src/main/resources/OSC_application.yaml`
```yaml
//--- 생략 ...
  #      mode: embedded  #always | never | embedded

server:
  port: 8103

external:
  coupon:
    base-url: http://coupon-service.msa.svc.cluster.local:8081
  point:
//--- 생략 ...
```
- 설명: Kafka/Consumer 배포 설정을 추가해 실행 환경을 고정한다.

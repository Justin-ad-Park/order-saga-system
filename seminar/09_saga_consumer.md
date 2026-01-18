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

## 코드 발췌 및 설명
- `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/application/service/ProcessOrderSagaEventService.java`: 이벤트 상태에 따라 confirm/compensate 분기 처리
```java
        if (sagaStatus == OrderSagaStatus.Reserved) {
            handleConfirm(orderId, info);
            return;
        }

        if (sagaStatus == OrderSagaStatus.Compensating) {
            handleCompensate(orderId, info);
        }
```
- 왜 필요한가: Reserved/Compensating 분기를 보여줘, 사가 보상 로직의 핵심을 이해시키기 좋다.

## 커밋 상세
### 3afbfb9 Comsumer 기본 프로젝트 및 기본 로직 구성
- 변경 요약: Comsumer 기본 프로젝트 및 기본 로직 구성
- 핵심 로직: 모듈/의존성 구성 변경
- 구조 변화: 소비자 모듈 또는 이벤트 처리 흐름 확장
- 주요 파일: `order-saga-consumer/build.gradle`, `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/OrderSagaConsumerApplication.java`
- 코드 발췌: `order-saga-consumer/build.gradle`
```diff
+plugins {
+    id 'org.springframework.boot'
+    id 'io.spring.dependency-management' version '1.1.5'
+    id 'java'
+}
+
+dependencies {
+    implementation 'org.springframework.boot:spring-boot-starter'
```
- 코드 발췌: `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/OrderSagaConsumerApplication.java`
```diff
+package com.example.ordersagaconsumer;
+
+import org.springframework.boot.autoconfigure.SpringBootApplication;
+import org.springframework.boot.builder.SpringApplicationBuilder;
+import org.springframework.kafka.annotation.EnableKafka;
+
+@EnableKafka
+@SpringBootApplication
```

### 0b73be2 ### Saga 컨슈머 confirm, compensate 로직 추가 ###
- 변경 요약: ### Saga 컨슈머 confirm, compensate 로직 추가 ###
- 핵심 로직: 이벤트 소비/처리 로직
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `order-saga-consumer/build.gradle`, `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/adapter/out/persistence/OrderSagaStatusJdbcAdapter.java`
- 변경 전/후 비교: `order-saga-consumer/build.gradle`
- diff 스타일
```diff
@@ -8,6 +8,7 @@ dependencies {
     implementation 'org.springframework.boot:spring-boot-starter'
     implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
     implementation 'org.springframework.boot:spring-boot-starter-jdbc'
+    implementation 'org.springframework.boot:spring-boot-starter-webflux'
     implementation 'org.springframework.kafka:spring-kafka'
     implementation 'org.springframework.boot:spring-boot-starter-json'
     implementation 'com.mysql:mysql-connector-j'
```
- 코드 발췌: `order-saga-consumer/build.gradle`
```diff
+    implementation 'org.springframework.boot:spring-boot-starter-webflux'
```
- 코드 발췌: `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/adapter/out/persistence/OrderSagaStatusJdbcAdapter.java`
```diff
+package com.example.ordersagaconsumer.adapter.out.persistence;
+
+import com.example.ordersagaconsumer.application.port.out.UpdateOrderSagaStatusPort;
+import com.example.ordersagaconsumer.domain.model.status.OrderSagaStatus;
+import org.springframework.jdbc.core.JdbcTemplate;
+import org.springframework.stereotype.Repository;
+
+@Repository
```

### a1f74d8 ### Consumer host Test ###
- 변경 요약: ### Consumer host Test ###
- 핵심 로직: 이벤트 소비/처리 로직
- 구조 변화: 소비자 모듈 또는 이벤트 처리 흐름 확장
- 주요 파일: `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/OrderSagaConsumerApplication.java`, `order-saga-consumer/src/main/resources/OSC_application.yaml`
- 변경 전/후 비교: `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/OrderSagaConsumerApplication.java`
- diff 스타일
```diff
@@ -10,7 +10,7 @@ public class OrderSagaConsumerApplication {
 
     public static void main(String[] args) {
         if (System.getProperty("spring.profiles.active") == null) {
-            System.setProperty("spring.profiles.active", "test");
+            System.setProperty("spring.profiles.active", "k8s-local");
         }
         new SpringApplicationBuilder(OrderSagaConsumerApplication.class)
                 .properties(
```
- 코드 발췌: `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/OrderSagaConsumerApplication.java`
```diff
+            System.setProperty("spring.profiles.active", "k8s-local");
```
- 코드 발췌: `order-saga-consumer/src/main/resources/OSC_application.yaml`
```diff
+order:
+  saga:
+    events:
+      topic: order-saga-events
+      consumer-group: order-saga-consumer-local
+
+---
+spring:
```

### 576a868 Comsumer 실행 시 profile 설정 안되는 오류 수정
- 변경 요약: Comsumer 실행 시 profile 설정 안되는 오류 수정
- 핵심 로직: 이벤트 소비 및 후속 처리 로직
- 구조 변화: 소비자 모듈 또는 이벤트 처리 흐름 확장
- 주요 파일: `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/OrderSagaConsumerApplication.java`, `order-saga-consumer/src/main/resources/OSC_application.yaml`
- 변경 전/후 비교: `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/OrderSagaConsumerApplication.java`
- diff 스타일
```diff
@@ -3,13 +3,21 @@ package com.example.ordersagaconsumer;
 import org.springframework.boot.autoconfigure.SpringBootApplication;
 import org.springframework.boot.builder.SpringApplicationBuilder;
 import org.springframework.kafka.annotation.EnableKafka;
+import java.util.Arrays;
 
 @EnableKafka
 @SpringBootApplication
 public class OrderSagaConsumerApplication {
 
     public static void main(String[] args) {
-        if (System.getProperty("spring.profiles.active") == null) {
+        String systemProfile = System.getProperty("spring.profiles.active");
+        boolean hasProfileArg = Arrays.stream(args)
+                .anyMatch(arg -> arg.startsWith("--spring.profiles.active="));
+        String envProfile = System.getenv("SPRING_PROFILES_ACTIVE");
```
- 코드 발췌: `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/OrderSagaConsumerApplication.java`
```diff
+import java.util.Arrays;
+        String systemProfile = System.getProperty("spring.profiles.active");
+        boolean hasProfileArg = Arrays.stream(args)
+                .anyMatch(arg -> arg.startsWith("--spring.profiles.active="));
+        String envProfile = System.getenv("SPRING_PROFILES_ACTIVE");
+
+        if ((systemProfile == null || systemProfile.isBlank())
+                && !hasProfileArg
```
- 코드 발췌: `order-saga-consumer/src/main/resources/OSC_application.yaml`
```diff
+    consumer:
+      group-id: order-saga-consumer-test
+      auto-offset-reset: earliest
+  port: 8083
+      consumer-group: order-saga-consumer-test
```

### 5a250f8 ### Saga Local & K8s + Host Consumer 테스트 완료 ###
- 변경 요약: ### Saga Local & K8s + Host Consumer 테스트 완료 ###
- 핵심 로직: 이벤트 소비/처리 로직
- 구조 변화: 소비자 모듈 또는 이벤트 처리 흐름 확장
- 주요 파일: `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/OrderSagaConsumerApplication.java`
- 변경 전/후 비교: `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/OrderSagaConsumerApplication.java`
- diff 스타일
```diff
@@ -3,23 +3,12 @@ package com.example.ordersagaconsumer;
 import org.springframework.boot.autoconfigure.SpringBootApplication;
 import org.springframework.boot.builder.SpringApplicationBuilder;
 import org.springframework.kafka.annotation.EnableKafka;
-import java.util.Arrays;
 
 @EnableKafka
 @SpringBootApplication
 public class OrderSagaConsumerApplication {
 
     public static void main(String[] args) {
-        String systemProfile = System.getProperty("spring.profiles.active");
-        boolean hasProfileArg = Arrays.stream(args)
-                .anyMatch(arg -> arg.startsWith("--spring.profiles.active="));
-        String envProfile = System.getenv("SPRING_PROFILES_ACTIVE");
-
```
- 코드 발췌: `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/OrderSagaConsumerApplication.java`
```java
package com.example.ordersagaconsumer;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
```

### 9e08ba1 ### Consumer K8s 배포 및 실행 스크립트 추사 ###
- 변경 요약: ### Consumer K8s 배포 및 실행 스크립트 추사 ###
- 핵심 로직: 이벤트 소비/처리 로직
- 구조 변화: 소비자 모듈 또는 이벤트 처리 흐름 확장
- 주요 파일: `order-saga-consumer/scripts/deploy_k8s.sh`, `order-saga-consumer/src/main/resources/OSC_application.yaml`
- 코드 발췌: `order-saga-consumer/scripts/deploy_k8s.sh`
```diff
+#!/usr/bin/env bash
+set -euo pipefail
+
+ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
+SERVICE_DIR="${ROOT_DIR}/order-saga-consumer"
+MANIFEST="${ROOT_DIR}/bin_k8s/order-saga-consumer.yaml"
+IMAGE_NAME="order-saga-consumer:local"
```
- 코드 발췌: `order-saga-consumer/src/main/resources/OSC_application.yaml`
```diff
+  port: 8103
```

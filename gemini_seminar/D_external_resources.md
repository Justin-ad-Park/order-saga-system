# Appendix D. 추천 외부 학습 자료

이 교육 자료는 MSA 및 EDA 기반 주문 시스템 개발의 핵심 개념과 구현 과정을 다룹니다. 하지만 각 주제는 방대하므로, 더 깊이 있는 학습을 위해서는 아래 추천 자료들을 참고하시기 바랍니다.

---

## 1. MSA (Microservice Architecture)

*   **마이크로서비스 패턴 (Microservice Patterns)** - Chris Richardson 저
    *   MSA 설계의 고전적인 교과서입니다. Saga, Outbox, API Gateway 등 다양한 패턴에 대한 심도 깊은 내용을 다룹니다.
    *   [공식 웹사이트](https://microservices.io/patterns/index.html)
*   **Building Microservices** - Sam Newman 저
    *   MSA 도입 시 고려해야 할 조직 문화, 기술적 도전, 실무적인 접근 방식에 대해 잘 설명되어 있습니다.

## 2. EDA (Event-Driven Architecture) 및 Kafka

*   **Designing Event-Driven Systems** - Ben Stopford 저
    *   EDA의 기본 원리, 디자인 패턴, 그리고 Kafka를 활용한 구현에 대해 심도 있게 다룹니다.
    *   [공식 웹사이트](https://www.confluent.io/designing-event-driven-systems/)
*   **Apache Kafka 공식 문서**
    *   Kafka의 개념, 아키텍처, API 등에 대한 가장 정확하고 최신 정보를 제공합니다.
    *   [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
*   **Event Sourcing 패턴**
    *   모든 상태 변경을 이벤트로 기록하는 패턴으로, EDA와 함께 사용될 때 강력한 시너지를 냅니다.
    *   [Microservices.io - Event Sourcing](https://microservices.io/patterns/data/event-sourcing.html)

## 3. Saga 패턴

*   **Saga 패턴 (Microservices.io)**
    *   Saga 패턴의 두 가지 주요 구현 방식(Choreography, Orchestration) 및 보상 트랜잭션에 대해 잘 정리되어 있습니다.
    *   [Microservices.io - Saga](https://microservices.io/patterns/data/saga.html)
*   **Outbox Pattern (Microservices.io)**
    *   Saga 패턴과 함께 사용되는 Outbox Pattern에 대한 설명입니다.
    *   [Microservices.io - Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html)

## 4. Hexagonal Architecture

*   **Ports and Adapters Architecture (Hexagonal Architecture)** - Alistair Cockburn
    *   원작자의 설명과 개념에 대한 이해를 돕는 자료입니다.
    *   [Alistair Cockburn - Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
*   **Architecture Test (ArchUnit)**
    *   코드로 아키텍처 규칙을 검증하는 ArchUnit 라이브러리의 공식 문서입니다.
    *   [ArchUnit Official Documentation](https://www.archunit.org/userguide/html/000_Introduction.html)

## 5. Istio (Service Mesh) 및 복원력

*   **Istio 공식 문서**
    *   Istio 설치, 설정, 기능에 대한 가장 정확한 정보를 제공합니다.
    *   [Istio Documentation](https://istio.io/latest/docs/)
*   **Circuit Breaker 패턴**
    *   Circuit Breaker 패턴의 일반적인 개념과 구현에 대한 설명입니다.
    *   [Microservices.io - Circuit Breaker](https://microservices.io/patterns/reliability/circuit-breaker.html)

## 6. Spring Boot 및 Java 관련

*   **Spring Boot 공식 문서**
    *   Spring Boot 애플리케이션 개발에 필요한 모든 정보를 담고 있습니다.
    *   [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
*   **Reactor (Reactive Programming)**
    *   Spring WebFlux와 함께 사용되는 리액티브 프로그래밍 라이브러리 Reactor의 공식 문서입니다.
    *   [Project Reactor Reference Guide](https://projectreactor.io/docs/core/release/reference/)

---
이 자료들을 통해 MSA 및 EDA의 각 개념을 더욱 심도 있게 학습하고, 실제 프로젝트에 적용하는 데 필요한 지식을 쌓으시길 바랍니다.

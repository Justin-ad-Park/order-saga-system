# MSA & EDA 기반 주문 시스템 개발 여정 (심화 & 실전)

## 교육 자료 목표
이 교육 자료는 MSA(Microservice Architecture)와 EDA(Event-Driven Architecture) 환경에서 **Saga 패턴**과 **서킷 브레이커** 등을 활용하여 어떻게 신뢰성 있는 분산 주문 시스템을 구축하는지에 대한 단계별 개발 과정을 제공합니다. 초급 개발자가 이론적 배경을 탄탄히 다지고 실제 코드의 진화 과정을 Git 히스토리와 연동하여 실전 감각을 키우는 것을 목표로 합니다.

## 학습 목표
*   MSA 및 EDA의 핵심 개념과 도입 필요성을 명확히 이해합니다.
*   Hexagonal Architecture가 분산 시스템 설계에 어떻게 적용되는지 학습합니다.
*   Git 커밋 히스토리를 통해 실제 시스템이 기능별로 어떻게 발전하는지 추적합니다.
*   Outbox Pattern, Kafka 기반 이벤트 발행/소비, Saga 패턴의 구현 과정을 상세히 살펴봅니다.
*   Istio Circuit Breaker를 활용한 장애 복원력 확보 방안을 익힙니다.
*   각 단계별 코드를 직접 실행하고 결과를 검증하며 실전적인 경험을 쌓습니다.

## 목차 (Table of Contents)

각 챕터를 순서대로 따라가며 시스템이 어떻게 진화했는지 학습해보세요. 각 챕터는 **개념 설명, 관련 Git 커밋, 핵심 코드 스니펫, 상세 실습 가이드**를 포함합니다.

*   **[Chapter 1: Order Orchestrator와 Hexagonal Architecture](./01_order_orchestrator.md)**
    *   **개념:** MSA의 기반이 되는 Hexagonal Architecture(Ports & Adapters)를 이해하고, `order-orchestrator`의 초기 설계 원칙을 살펴봅니다.
    *   **학습:** 유연하고 테스트하기 쉬운 시스템 구조가 왜 중요한지, ArchUnit을 통해 어떻게 아키텍처 규칙을 강제하는지 학습합니다.

*   **[Chapter 2: MSA 확장을 위한 Coupon Service 추가](./02_coupon_service_msa.md)**
    *   **개념:** 단일 서비스에서 벗어나 첫 번째 마이크로서비스인 `coupon-service`를 추가하며 MSA로의 전환 과정을 이해합니다.
    *   **학습:** 초기 MSA 간 동기(HTTP) 통신의 장단점과 함께, 서비스 분리의 실질적인 이점과 과제를 경험합니다.

*   **[Chapter 3: Outbox Pattern으로 신뢰성 확보](./03_outbox_pattern.md)**
    *   **개념:** 분산 시스템에서 발생하는 데이터 정합성 문제를 해결하기 위한 Outbox Pattern의 원리와 도입 배경을 학습합니다.
    *   **학습:** 트랜잭션과 이벤트 발행의 원자성을 어떻게 보장하는지, Outbox 테이블이 Saga 상태 추적 및 신뢰성 있는 이벤트 발행에 어떻게 활용되는지 알아봅니다.

*   **[Chapter 4: Kafka로 이벤트 기반 백본(Backbone) 구축](./04_kafka_setup.md)**
    *   **개념:** 서비스 간 느슨한 결합(loose coupling)을 위해 Apache Kafka를 도입하여 이벤트 기반 아키텍처의 핵심 인프라를 구축하는 과정을 이해합니다.
    *   **학습:** 쿠버네티스 환경에 Kafka를 배포하고, 토픽 생성 및 기본 동작을 확인하는 방법을 실습합니다.

*   **[Chapter 5: Kafka에 이벤트 발행(Publish)하기](./05_event_publishing.md)**
    *   **개념:** `order-orchestrator`에서 발생한 Saga 이벤트를 Kafka 토픽으로 발행하는 로직을 학습합니다.
    *   **학습:** Outbox에 기록된 Saga 상태를 기반으로 이벤트를 발행하는 과정과 Kafka Publisher의 구현을 살펴봅니다.

*   **[Chapter 6: Saga Consumer로 이벤트 소비(Consume)하기](./06_saga_consumer.md)**
    *   **개념:** Kafka에 발행된 Saga 이벤트를 수신하여 실제 비즈니스 로직(confirm/compensate)을 처리하는 `order-saga-consumer`의 역할을 이해합니다.
    *   **학습:** `KafkaListener`를 이용한 이벤트 소비 방법과 컨슈머의 이벤트 처리 로직을 상세히 분석합니다.

*   **[Chapter 7: Saga 보상 트랜잭션 (Compensating Transaction) 구현](./07_saga_compensation.md)**
    *   **개념:** 분산 트랜잭션의 핵심인 Saga 패턴의 보상 로직과 멱등성(Idempotency)의 중요성을 학습합니다.
    *   **학습:** 컨슈머에서 Saga 상태에 따라 `confirm` 또는 `compensate` 로직을 분기하여 호출하는 구현과, 타이밍 이슈 해결을 위한 `reservation` 테이블 활용 방안을 깊이 있게 다룹니다.

*   **[Chapter 8: Istio 서킷 브레이커로 안정성 강화](./08_istio_circuit_breaker.md)**
    *   **개념:** 특정 서비스의 장애가 시스템 전체로 전파되는 것을 방지하는 서킷 브레이커 패턴과 서비스 메시 Istio의 역할을 이해합니다.
    *   **학습:** 애플리케이션 코드 변경 없이 Istio `DestinationRule` 및 `VirtualService`를 통해 네트워크 레벨에서 장애 격리 및 회복 기능을 구성하는 방법을 실습합니다.

*   **[Chapter 9: 서킷 브레이커 동작 테스트 및 안정성 검증](./09_resilience_testing.md)**
    *   **개념:** 구축한 서킷 브레이커가 실제 장애 상황(지연, 타임아웃)에서 의도한 대로 동작하고, 시스템이 안정적으로 복구되는지 검증하는 중요성을 학습합니다.
    *   **학습:** 강제 지연 로직(Decorator 패턴)을 활용한 테스트 환경 구성, `bin_istio_test` 스크립트를 통한 서킷 브레이커 동작 확인 및 보상 트랜잭션의 최종 상태를 검증하는 실전 테스트를 수행합니다.

## 부록 (Appendix)
*   **[A. 전체 시스템 아키텍처 다이어그램](./A_architecture_diagram.md)**
*   **[B. 공통 스크립트 사용법](./B_common_scripts.md)**
*   **[C. 핵심 용어 정리](./C_glossary.md)**
*   **[D. 추천 외부 학습 자료](./D_external_resources.md)**

# MSA & EDA 기반 주문 시스템 개발 여정

안녕하세요! 이 교육 자료는 MSA(Microservice Architecture)와 EDA(Event-Driven Architecture) 환경에서 **Saga 패턴**과 **서킷 브레이커** 등을 활용하여 어떻게 신뢰성 있는 분산 주문 시스템을 구축하는지에 대한 단계별 개발 과정을 담고 있습니다.

Git 히스토리를 기반으로 각 기능이 추가되고 시스템이 발전하는 과정을 시간 순서대로 재구성하였으며, 각 챕터는 특정한 기술적 과제를 해결하는 과정을 상세히 설명합니다.

## 목차 (Table of Contents)

각 챕터를 순서대로 따라가며 시스템이 어떻게 진화했는지 학습해보세요.

*   **[Chapter 1: Order Orchestrator와 Hexagonal Architecture](./01_order_orchestrator.md)**
    *   프로젝트의 시작점인 `order-orchestrator` 서비스의 초기 구조를 살펴봅니다.
    *   유연하고 테스트하기 쉬운 설계를 위한 Hexagonal Architecture의 기본 개념과 적용 사례를 알아봅니다.

*   **[Chapter 2: MSA 확장을 위한 Coupon Service 추가](./02_coupon_service_msa.md)**
    *   단일 서비스에서 벗어나 첫 번째 마이크로서비스인 `coupon-service`를 추가합니다.
    *   두 서비스가 동기(HTTP) 방식으로 상호작용하는 과정을 살펴보고 MSA로의 전환에 따른 과제를 알아봅니다.

*   **[Chapter 3: Outbox Pattern으로 신뢰성 보장하기](./03_outbox_pattern.md)**
    *   분산 시스템의 고질적인 문제인 데이터 정합성(consistency) 문제를 해결하기 위해 Outbox Pattern을 도입합니다.
    *   서비스의 로컬 트랜잭션과 메시지 발행을 어떻게 원자적으로 묶는지 알아봅니다.

*   **[Chapter 4: Kafka로 이벤트 기반 백본(Backbone) 구축하기](./04_kafka_setup.md)**
    *   서비스 간의 강한 결합(tight coupling)을 해소하기 위해 Apache Kafka를 도입하여 이벤트 기반 아키텍처의 토대를 마련합니다.
    *   쿠버네티스 환경에 Kafka를 구축하고 기본적인 테스트를 수행하는 과정을 살펴봅니다.

*   **[Chapter 5: Kafka에 이벤트 발행(Publish)하기](./05_event_publishing.md)**
    *   Outbox Pattern과 스케줄러를 결합하여 데이터베이스에 저장된 메시지(이벤트)를 신뢰성 있게 Kafka로 발행하는 'Polling Publisher' 로직을 구현합니다.

*   **[Chapter 6: Saga Consumer로 이벤트 소비(Consume)하기](./06_saga_consumer.md)**
    *   발행된 이벤트를 수신하여 실질적인 비즈니스 로직을 처리하는 `order-saga-consumer` 서비스를 구현합니다.
    *   Saga 코디네이터로서 다른 서비스들을 어떻게 조율하는지 알아봅니다.

*   **[Chapter 7: Saga 보상 트랜잭션 (Compensating Transaction) 구현](./07_saga_compensation.md)**
    *   Saga 패턴의 핵심인 보상 트랜잭션의 개념을 이해하고, 분산 트랜잭션 실패 시 시스템의 상태를 되돌리는 로직을 구현합니다.
    *   멱등성(Idempotency) 확보의 중요성에 대해 알아봅니다.

*   **[Chapter 8: Istio 서킷 브레이커로 안정성 강화하기](./08_istio_circuit_breaker.md)**
    *   특정 서비스의 장애가 시스템 전체로 전파되는 것을 막기 위해 서비스 메시 Istio를 활용하여 서킷 브레이커를 구현합니다.
    *   애플리케이션 코드 변경 없이 네트워크 레벨에서 안정성을 확보하는 방법을 알아봅니다.

*   **[Chapter 9: 서킷 브레이커 동작 테스트 및 안정성 검증](./09_resilience_testing.md)**
    *   구축한 서킷 브레이커가 실제 장애 상황에서 의도한 대로 동작하는지 검증하는 테스트 시나리오와 환경을 구축합니다.
    *   테스트 과정에서 발견된 타이밍 이슈를 해결하며 분산 시스템 테스트의 중요성을 알아봅니다.

# GEMINI_KOR.md - order-saga-system

## 프로젝트 개요

이 프로젝트는 마이크로서비스 아키텍처(MSA) 및 이벤트 기반 아키텍처(EDA) 환경에서 **Saga 오케스트레이션 패턴**을 시연하고 교육하기 위해 설계된 멀티 모듈 Java 프로젝트입니다. 이 시스템은 여러 서비스에 걸친 분산 트랜잭션을 처리하여 데이터 일관성을 보장하는 전자상거래 주문 프로세스를 시뮬레이션합니다.

프로젝트의 핵심은 주문 생성을 위한 Saga를 관리하는 `order-orchestrator`입니다. 이 서비스는 `coupon-service` 및 `point-service`와 협력하여 리소스를 예약합니다. 이벤트는 Apache Kafka 토픽으로 발행되며, `order-saga-consumer`는 이러한 이벤트를 구독하여 Saga를 최종 상태(확정 또는 보상)로 이끕니다.

**주요 기술:**
*   **백엔드:** Java 17, Spring Boot 3.3.x
*   **아키텍처:** 마이크로서비스 아키텍처(MSA), 이벤트 기반 아키텍처(EDA), 헥사고날 아키텍처(Ports and Adapters)
*   **메시징:** Apache Kafka
*   **데이터베이스:** MySQL (서비스당 하나), H2
*   **빌드:** Gradle (멀티 프로젝트)
*   **배포:** Docker, Kubernetes
*   **서비스 메시:** Istio (서킷 브레이커와 같은 복원력 패턴을 위함)

**핵심 모듈:**
*   `order-orchestrator`: 주문 Saga를 오케스트레이션하는 중앙 서비스.
*   `coupon-service`: 쿠폰 관련 로직(예약, 확정, 보상)을 관리.
*   `point-service`: 포인트 관련 로직을 관리.
*   `order-saga-consumer`: Kafka 토픽을 구독하여 Saga 이벤트(확정/보상)를 처리.
*   `common`: API 응답 및 상태 Enum과 같은 공통 클래스를 위한 공유 라이브러리.

---

## 빌드 및 실행

이 프로젝트는 Gradle 래퍼를 사용하므로 로컬에 Gradle을 설치할 필요가 없습니다. 저장소에는 빌드, 배포, 테스트를 자동화하기 위한 수많은 쉘 스크립트(`bin_*` 폴더)가 포함되어 있습니다.

### 빌드 및 사전 점검

변경 사항을 제출하기 전에 전체 사전 점검(preflight check)을 실행하여 유효성을 검사하는 것이 매우 중요합니다. 이 명령어는 저장소를 빌드하고, 모든 테스트(ArchUnit 아키텍처 테스트 포함)를 실행하며, 타입 에러를 확인하고, 코드를 린팅합니다.

```bash
./gradlew check
```

### 로컬 테스트 실행

`bin_test` 디렉토리의 스크립트는 로컬 머신에서 서비스를 실행하고 테스트하기 위해 설계되었습니다.

1.  **환경 준비:** Docker가 실행 중인지 확인합니다. 스크립트가 필요한 컨테이너(MySQL, Kafka)를 관리합니다.
2.  **전체 로컬 테스트 시나리오 실행:** 이 스크립트는 필요한 인프라를 시작하고, 서비스를 빌드하며, 종단 간 테스트를 위해 로컬에서 실행합니다.
    ```bash
    ./bin_test/01_prepare_local_order_saga_test.sh
    ```
3.  **로컬 서비스 중지:**
    ```bash
    ./bin_test/_01_stop_local_order_orchestrator_test.sh
    ```

### 쿠버네티스에서 실행

이 프로젝트는 쿠버네티스 클러스터(예: Docker Desktop의 쿠버네티스 또는 Minikube)에 완전히 배포되도록 설계되었습니다.

1.  **쿠버네티스 환경 초기화:** 이 스크립트는 MySQL, 모든 MSA, Kafka를 배포하고 Istio 구성을 적용합니다.
    ```bash
    ./bin_k8s/00_init_k8s.sh
    ```
2.  **통합 테스트 실행:** K8s 환경을 초기화한 후, 배포된 서비스에 대해 통합 테스트를 실행할 수 있습니다.
    ```bash
    ./bin_k8s/08_integrationTest.sh
    ```
3.  **쿠버네티스 리소스 정리:**
    ```bash
    ./bin_k8s/_00_kill_k8s.sh
    ```

---

## 개발 규칙

*   **헥사고날 아키텍처 (Hexagonal Architecture):** 이 프로젝트는 Ports and Adapters 패턴을 엄격하게 따릅니다.
    *   **도메인 (Domain):** 외부 의존성이 없는 핵심 비즈니스 로직.
    *   **애플리케이션 (Application):** 도메인 로직을 오케스트레이션하는 유스케이스.
    *   **어댑터 (Adapters):** 웹 컨트롤러/컨슈머를 위한 `in` 어댑터와 영속성, 외부 서비스 클라이언트 등을 위한 `out` 어댑터.
*   **코드로서의 아키텍처 (Architecture as Code):** **ArchUnit**을 사용하여 빌드 시 헥사고날 아키텍처 규칙을 강제합니다. 관련 테스트는 각 서비스의 테스트 소스 셋 내 `archunit` 패키지에 있습니다.
*   **서비스당 데이터베이스 (Database-per-Service):** 각 마이크로서비스는 자체 전용 MySQL 데이터베이스를 가져 느슨한 결합을 보장합니다.
*   **교육 초점:** 이 프로젝트에는 `seminar`, `seminar2_MSA_EDA_Process`, `gemini_seminar` 폴더에 광범위한 문서와 스크립트가 포함되어 있습니다. 이는 프로젝트의 단계별 발전 과정과 개념을 이해하는 데 매우 중요합니다.
*   **API 명세:** API 계약은 `docs/project_architechture.md`에 문서화되어 있습니다.
*   **설정 관리:** 서비스 설정은 `application.yaml` 파일을 통해 관리되며, 다양한 환경(`test`, `dev`)을 위한 프로파일이 있습니다.

!Q!!!!!!!# GEMINI.md - order-saga-system

## Project Overview

This is a multi-module Java project designed to demonstrate and teach the **Saga Orchestration Pattern** in a Microservice Architecture (MSA) and Event-Driven Architecture (EDA). The system simulates an e-commerce order process, handling distributed transactions across multiple services to ensure data consistency.

The core of the project is the `order-orchestrator`, which manages the saga for placing an order. It coordinates with `coupon-service` and `point-service` to reserve resources. Events are published to an Apache Kafka topic, and an `order-saga-consumer` subscribes to these events to drive the saga to its final state (Confirmed or Compensated).

**Key Technologies:**
*   **Backend:** Java 17, Spring Boot 3.3.x
*   **Architecture:** Microservice Architecture (MSA), Event-Driven Architecture (EDA), Hexagonal Architecture (Ports and Adapters)
*   **Messaging:** Apache Kafka
*   **Database:** MySQL (one per service), H2
*   **Build:** Gradle (Multi-project)
*   **Deployment:** Docker, Kubernetes
*   **Service Mesh:** Istio (for resilience patterns like Circuit Breaker)

**Core Modules:**
*   `order-orchestrator`: The central service that orchestrates the order saga.
*   `coupon-service`: Manages coupon logic (reservation, confirmation, compensation).
*   `point-service`: Manages loyalty point logic.
*   `order-saga-consumer`: Subscribes to Kafka topics to process saga events (confirm/compensate).
*   `common`: A shared library for common classes like API responses and status enums.

---

## Building and Running

This project uses a Gradle wrapper, so a local Gradle installation is not required. The repository contains numerous shell scripts (`bin_*` folders) to automate building, deployment, and testing.

### Building and Preflight Checks

Before submitting any changes, it is crucial to validate them by running the full suite of preflight checks. This command will build the repository, run all tests (including ArchUnit architecture tests), check for type errors, and lint the code.

```bash
./gradlew check
```

### Running Locally for Testing

The scripts in the `bin_test` directory are designed for running and testing the services on your local machine.

1.  **Prepare Environment:** Ensure Docker is running. The scripts will manage the necessary containers (MySQL, Kafka).
2.  **Run Full Local Test Scenario:** This script starts the necessary infrastructure, builds the services, and runs them locally for end-to-end testing.
    ```bash
    ./bin_test/01_prepare_local_order_saga_test.sh
    ```
3.  **Stop Local Services:**
    ```bash
    ./bin_test/_01_stop_local_order_orchestrator_test.sh
    ```

### Running on Kubernetes

The project is designed to be fully deployed on a Kubernetes cluster (e.g., Docker Desktop's Kubernetes or Minikube).

1.  **Initialize Kubernetes Environment:** This script deploys MySQL, all MSAs, Kafka, and applies Istio configurations.
    ```bash
    ./bin_k8s/00_init_k8s.sh
    ```
2.  **Run Integration Test:** After initializing the K8s environment, you can run integration tests against the deployed services.
    ```bash
    ./bin_k8s/08_integrationTest.sh
    ```
3.  **Clean Up Kubernetes Resources:**
    ```bash
    ./bin_k8s/_00_kill_k8s.sh
    ```

---

## Development Conventions

*   **Hexagonal Architecture:** The project strictly follows the Ports and Adapters pattern.
    *   **Domain:** Core business logic with no external dependencies.
    *   **Application:** Use cases that orchestrate domain logic.
    *   **Adapters:** `in` adapters for web controllers/consumers and `out` adapters for persistence, external service clients, etc.
*   **Architecture as Code:** **ArchUnit** is used to enforce the Hexagonal Architecture rules at build time. Tests for this are located in the `archunit` package within each service's test source set.
*   **Database-per-Service:** Each microservice has its own dedicated MySQL database, ensuring loose coupling.
*   **Educational Focus:** The project contains extensive documentation and scripts in the `seminar`, `seminar2_MSA_EDA_Process`, and `gemini_seminar` folders. These are crucial for understanding the step-by-step evolution and concepts of the project.
*   **API Specification:** API contracts are documented in `docs/project_architechture.md`.
*   **Configuration Management:** Service configuration is managed via `application.yaml` files, with profiles for different environments (`test`, `dev`).

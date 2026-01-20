# 01. Problem -> MSA baseline

## Goal
Show why a single transaction is not enough and how the project splits into services.

## Core flow
- A single order touches coupon and point.
- If one side fails, a single transaction cannot roll back the others.
- The system moves to MSA and prepares for SAGA coordination.

## Architecture hint (module split)
`settings.gradle`
```gradle
rootProject.name = 'order-saga-system'

include 'order-orchestrator'
include 'order-saga-consumer'
include 'common'
include 'coupon-service'
include 'point-service'
```

## Hands-on checkpoints
- Read: `readme.md`, `project_desc.md`
- Confirm modules and build: `./gradlew projects`

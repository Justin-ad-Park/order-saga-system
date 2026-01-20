# 01. 문제 인식 -> MSA 기본 구조

## 목표
단일 트랜잭션의 한계를 보여주고, 서비스 분리가 필요한 이유를 이해한다.

## 핵심 흐름
- 한 주문이 쿠폰/포인트를 동시에 건드린다.
- 한쪽이 실패하면 단일 트랜잭션으로 다른 쪽을 되돌릴 수 없다.
- MSA로 분리하고, SAGA 오케스트레이션을 준비한다.

## 아키텍처 힌트(모듈 분리)
`settings.gradle`
```gradle
rootProject.name = 'order-saga-system'

include 'order-orchestrator'
include 'order-saga-consumer'
include 'common'
include 'coupon-service'
include 'point-service'
```

## 실습 체크포인트
- 읽기: `readme.md`, `project_desc.md`
- 모듈 확인/빌드: `./gradlew projects`

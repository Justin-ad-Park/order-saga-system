# 10. 멱등성, 오류 메시지, API 확장

## 목표
- API 멱등성과 오류 처리 정책을 이해한다.

## 스토리라인
- 중복 요청과 보상 요청이 반복되면서, 오류 메시지와 멱등 처리가 중요해짐.

## 관련 커밋
- `542ed97`, `091c2a7`, `66c93ca`, `35b85e3`, `605354d`, `7d9e662`

## 커밋 변경 요약
| 커밋 | 주요 변경 요약 | 체크아웃 |
| --- | --- | --- |
| `542ed97` | ### Coupon-service confirm API 추가 ### | `git checkout 542ed97` |
| `091c2a7` | coupon-service에 보상(compensateCoupon) API 추가 | `git checkout 091c2a7` |
| `66c93ca` | confirm, compansate API를 point-service에도 동일한 방식으로 추가 | `git checkout 66c93ca` |
| `35b85e3` | API 응답 에러 명시적으로 변경 중 | `git checkout 35b85e3` |
| `605354d` | coupon 서비스에도 오류 메시지 명시적으로 리턴하도록 확장 | `git checkout 605354d` |
| `7d9e662` | 이미 확정한 point, coupon 중복 확정 시 오류없이 처리(멱등성) | `git checkout 7d9e662` |

## 핵심 개념
- confirm/compensate API 설계
- 멱등성 정책(이미 처리된 요청)

## 기술/기능/프로세스
- 기술: 예외 처리, HTTP 상태 코드 설계
- 기능: 멱등 confirm/compensate, 명확한 오류 메시지
- MSA: 쿠폰/포인트 공통 정책 정립
- EDA: 재시도/중복 이벤트 대비
## 데모/실습
- HTTP 테스트 파일 확인: `coupon-service/src/test/resources/01_couponServiceTest.http`, `point-service/src/test/resources/01_pointServiceTest.http`

## 커밋 상세
### 542ed97 ### Coupon-service confirm API 추가 ###
- 주요 변경: ### Coupon-service confirm API 추가 ###
- 핵심 코드: `order-saga-consumer/src/main/java/com/example/ordersagaconsumer/config/KafkaConsumerConfig.java`
```java
//--- 생략 ...
```
- 설명: 핵심 흐름을 구성하는 로직을 추가한다.


# 테스트 데이터셋 명명 규칙

## 쿠폰/포인트 번호 (지연/타임아웃 테스트)
- 지연 강제(prefix): `CPN-INT-FORCE-DELAY*`, `PNT-INT-FORCE-DELAY*`
- 정상 시작: `CPN-INT-OK-START`, `PNT-INT-OK-START`
- 회로 open 직후 재시도: `CPN-INT-AFTER-OPEN`, `PNT-INT-AFTER-OPEN`
- 회로 복구 후: `CPN-INT-AFTER-RECOVER`, `PNT-INT-AFTER-RECOVER`

## 데이터 정의 위치
- 쿠폰: `coupon-service/src/main/resources/coupon_schema.sql`
- 포인트: `point-service/src/main/resources/point_schema.sql`

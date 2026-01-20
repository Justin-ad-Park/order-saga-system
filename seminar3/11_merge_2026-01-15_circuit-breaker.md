# 11. circuit-breaker -> main

## 시점
- 2026-01-15

## 비교 기준
- 직전 main 상태: `37e61004ac251ded3b189e1dc754269fddc0db6a`
- 브랜치 tip: `4b031ed`

## 주요 변경(커밋 메시지 기반)
- *** Timeout Test용 강제 지연 로직을 SRP, OCP 등을 적용해 Decorator 패턴으로 분리. test, dev Profile에서만 사용하도록 변경

## MSA + EDA + SAGA 관점 요약
- 쿠폰 서비스 변경
- 포인트 서비스 변경

## 연결된 로직 흐름
- 유스케이스/서비스 처리 -> 쿠폰 서비스 처리 -> 포인트 서비스 처리

## 핵심 로직 스니펫(머지 시점 기준)
- `coupon-service/src/main/java/com/example/couponservice/application/service/ReserveCouponDelayDecorator.java`
```java
package com.example.couponservice.application.service;

import com.example.couponservice.application.port.in.ReserveCouponUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Primary
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class ReserveCouponDelayDecorator implements ReserveCouponUseCase {

    private final ReserveCouponService delegate;

    @Value("${circuit-test.coupon.delay-enabled:false}")
    private boolean delayEnabled;
    @Value("${circuit-test.coupon.delay-prefix:}")
    private String delayPrefix;
    @Value("${circuit-test.coupon.delay-ms:0}")
    private long delayMs;

    @Override
    public void reserve(String couponNumber, String orderId) {
        maybeDelay(couponNumber);
        delegate.reserve(couponNumber, orderId);
    }

    private void maybeDelay(String couponNumber) {
        if (!delayEnabled) {
            return;
        }
        if (delayMs <= 0 || delayPrefix == null || delayPrefix.isBlank()) {
            return;
        }
        if (!couponNumber.startsWith(delayPrefix)) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Delay interrupted", ex);
        }
    }
}
```

- `point-service/src/main/java/com/example/pointservice/application/service/ReservePointDelayDecorator.java`
  - 아래 Annotation에 의해 "dev", "test"에서는 ReservePointUseCase에 ReservePointDelayDecorator Bean이 주입됨
  - @Primary
  @Profile({"dev", "test"})
```java
package com.example.pointservice.application.service;

import com.example.pointservice.application.port.in.ReservePointUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Primary
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class ReservePointDelayDecorator implements ReservePointUseCase {

    private final ReservePointService delegate;

    @Value("${circuit-test.point.delay-enabled:false}")
    private boolean delayEnabled;
    @Value("${circuit-test.point.delay-prefix:}")
    private String delayPrefix;
    @Value("${circuit-test.point.delay-ms:0}")
    private long delayMs;

    @Override
    public void reserve(String pointNumber, String orderId) {
        maybeDelay(pointNumber);
        delegate.reserve(pointNumber, orderId);
    }

    private void maybeDelay(String pointNumber) {
        if (!delayEnabled) {
            return;
        }
        if (delayMs <= 0 || delayPrefix == null || delayPrefix.isBlank()) {
            return;
        }
        if (!pointNumber.startsWith(delayPrefix)) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Delay interrupted", ex);
        }
    }
}
```
- `point-service/src/main/java/com/example/pointservice/application/service/ReservePointService.java`
```java
package com.example.pointservice.application.service;

import com.example.pointservice.application.port.in.CompensatePointUseCase;
import com.example.pointservice.application.port.in.ConfirmPointUseCase;
import com.example.pointservice.application.port.in.ReservePointUseCase;
import com.example.pointservice.application.port.out.LoadPointPort;
import com.example.pointservice.application.port.out.SavePointPort;
import com.example.pointservice.domain.model.Point;
import com.example.pointservice.domain.model.status.PointStatus;
import jakarta.transaction.Transactional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservePointService implements ReservePointUseCase, ConfirmPointUseCase, CompensatePointUseCase {

    private final LoadPointPort loadPointPort;
    private final SavePointPort savePointPort;

    @Override
    public void reserve(String pointNumber, String orderId) {
        updateStatus(pointNumber, PointStatus.RESERVED, this::validateReservable);
    }

    @Override
    public void confirm(String pointNumber, String orderId) {
        Point point = loadPointPort.loadPoint(pointNumber)
                .orElseThrow(() -> new IllegalArgumentException("포인트를 찾을 수 없습니다: " + pointNumber));
        if (point.status() == PointStatus.USED) {
            return;
        }
        validateConfirmable(point);

        Point updated = new Point(
                point.pointNumber(),
                PointStatus.USED,
                point.issuedAt(),
                point.expiredAt()
        );
        savePointPort.save(updated);
    }

    @Override
    public void compensatePoint(String pointNumber, String orderId) {
        Point point = loadPointPort.loadPoint(pointNumber)
                .orElse(null);
        if (point == null) {
            return;
        }
        if (point.status() == PointStatus.USED) {
            throw new IllegalStateException("보상 불가능한 포인트입니다: " + point.pointNumber());
        }
        if (point.status() != PointStatus.RESERVED) {
            return;
        }

        Point updated = new Point(
                point.pointNumber(),
                PointStatus.AVAILABLE,
                point.issuedAt(),
                point.expiredAt()
        );
        savePointPort.save(updated);
    }

    private void updateStatus(
            String pointNumber,
            PointStatus targetStatus,
            Consumer<Point> validator
    ) {
        Point point = loadPointPort.loadPoint(pointNumber)
                .orElseThrow(() -> new IllegalArgumentException("포인트를 찾을 수 없습니다: " + pointNumber));

        validator.accept(point);

        Point updated = new Point(
                point.pointNumber(),
                targetStatus,
                point.issuedAt(),
                point.expiredAt()
        );

        savePointPort.save(updated);
    }

    private void validateReservable(Point point) {
        if (!point.isAvailable()) {
            throw new IllegalStateException("예약 불가능한 포인트입니다: " + point.pointNumber());
        }
    }

    private void validateConfirmable(Point point) {
        if (point.status() != PointStatus.RESERVED) {
            throw new IllegalStateException("확정 불가능한 포인트입니다: " + point.pointNumber());
        }
    }
}
```
- `coupon-service/src/main/resources/coupon_application.yaml`
```yaml
# src/main/resources/application.yml
spring:
  profiles:
    active: test

---
spring:
  config:
    activate:
      on-profile: test

  datasource:
    url: jdbc:mysql://localhost:3307/coupon_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
    username: coupon_user
    password: coupon_pw

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    defer-datasource-initialization: true

  sql:
    init:
      mode: always
      schema-locations: classpath:coupon_schema.sql
  #    init:
  #      mode: embedded  #always | never | embedded

server:
  port: 8081

circuit-test:
  coupon:
    delay-enabled: true
    delay-prefix: CPN-INT-CIRCUIT-ON
    delay-ms: 8000

---
spring:
  config:
    activate:
      on-profile: dev

  datasource:
    url: jdbc:mysql://mysql.msa.svc.cluster.local:3306/coupon_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
    username: coupon_user
    password: ${COUPON_DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    defer-datasource-initialization: true

  sql:
    init:
      mode: always
      schema-locations: classpath:coupon_schema.sql
  #    init:
  #      mode: embedded  #always | never | embedded

server:
  port: 8081

circuit-test:
  coupon:
    delay-enabled: true
    delay-prefix: CPN-INT-CIRCUIT-ON
    delay-ms: 8000
```
- `point-service/src/main/resources/point_application.yaml`
```yaml
# src/main/resources/application.yml
spring:
  profiles:
    active: test

---
spring:
  config:
    activate:
      on-profile: test

  datasource:
    url: jdbc:mysql://localhost:3307/point_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
    username: point_user
    password: point_pw

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    defer-datasource-initialization: true

  sql:
    init:
      mode: always
      schema-locations: classpath:point_schema.sql
  #    init:
  #      mode: embedded  #always | never | embedded

server:
  port: 8082

circuit-test:
  point:
    delay-enabled: true
    delay-prefix: PNT-INT-CIRCUIT-ON
    delay-ms: 8000

---
spring:
  config:
    activate:
      on-profile: dev

  datasource:
    url: jdbc:mysql://mysql.msa.svc.cluster.local:3306/point_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
    username: point_user
    password: ${POINT_DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    defer-datasource-initialization: true

  sql:
    init:
      mode: always
      schema-locations: classpath:point_schema.sql
  #    init:
  #      mode: embedded  #always | never | embedded

server:
  port: 8082

circuit-test:
  point:
    delay-enabled: true
    delay-prefix: PNT-INT-CIRCUIT-ON
    delay-ms: 8000
```

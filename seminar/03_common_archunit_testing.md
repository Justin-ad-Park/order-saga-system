# 03. 공통 모듈과 아키텍처 테스트

## 목표
- 공통 모듈의 역할과 ArchUnit을 통한 구조 검증을 이해한다.

## 스토리라인
- 모듈 간 의존성이 무너지기 시작하면서 구조 검증 도구가 필요해짐.

## 관련 커밋
- `e37883c`, `868aa6f`, `1475eba`, `6e8df39`

## 커밋 변경 요약
| 커밋 | 주요 변경 요약 | 체크아웃 |
| --- | --- | --- |
| `e37883c` | ### Common 모듈 추가 ###################### | `git checkout e37883c` |
| `868aa6f` | Archunit 검증 테스트 추가 | `git checkout 868aa6f` |
| `1475eba` | ArchitectureUnit 테스트 추가 | `git checkout 1475eba` |
| `6e8df39` | Archunit 중복제거 리팩토링 | `git checkout 6e8df39` |

## 핵심 개념
- common 모듈로 상태 모델/공통 DTO 공유
- ArchUnit으로 계층 규칙 강제

## 기술/기능/프로세스
- 기술: ArchUnit, common 모듈
- 기능: 계층/의존성 규칙 검증
- MSA: 모듈 경계 강제
- EDA: 이후 단계에서 이벤트 흐름 검증에 확장 가능
## 데모/실습
- ArchUnit 테스트 확인: `order-orchestrator/src/test/java/.../ArchitectureTest4OrderOrchestrator.java`

## 코드 발췌 및 설명
- `order-orchestrator/src/test/java/com/example/orderorchestrator/archunit/ArchitectureTest4OrderOrchestrator.java`: ArchUnit으로 헥사고날 규칙을 통과시키는 테스트
```java
@AnalyzeClasses(
        packages = ArchitectureTest4OrderOrchestrator.BASE_PACKAGE,
        importOptions = { ImportOption.DoNotIncludeTests.class }
)
public class ArchitectureTest4OrderOrchestrator extends HexagonalArchitectureTestTemplate {

    static final String BASE_PACKAGE = "com.example.orderorchestrator";

    @Override
    protected String basePackage() {
        return BASE_PACKAGE;
    }
}
```
- 왜 필요한가: 아키텍처 규칙을 자동으로 검증하는 장치를 보여줘, 구조가 무너지지 않도록 하는 이유를 설명할 수 있다.

## 커밋 상세
### e37883c ### Common 모듈 추가 ######################
- 변경 요약: ### Common 모듈 추가 ######################
- 핵심 로직: 모듈/의존성 구성 변경
- 구조 변화: 공통 모듈 추가 및 의존성 정리
- 주요 파일: `common/build.gradle`, `common/src/main/java/com/example/common/api/ApiError.java`
- 코드 발췌: `common/build.gradle`
```diff
+// org.springframework.boot 플러그인 절대 적용하지 않기
+plugins {
+    id 'java-library'
+}
+
+group = 'com.example'
+version = '0.0.1-SNAPSHOT'
```
- 코드 발췌: `common/src/main/java/com/example/common/api/ApiError.java`
```diff
+package com.example.common.api;
+
+// 공통 에러 DTO (web 계층)
+public class ApiError {
+    private final String code;
+    private final String message;
+    private ApiError(String code, String message) { this.code = code; this.message = message; }
+    public String getCode() { return code; }
```

### 868aa6f Archunit 검증 테스트 추가
- 변경 요약: Archunit 검증 테스트 추가
- 핵심 로직: 아키텍처 규칙 테스트
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `order-orchestrator/src/test/java/com/example/orderorchestrator/archunit/ArchitectureTest.java`
- 코드 발췌: `order-orchestrator/src/test/java/com/example/orderorchestrator/archunit/ArchitectureTest.java`
```diff
+        packages = "com.example.orderorchestrator",
+        importOptions = { ImportOption.DoNotIncludeTests.class }
+    private static final String PORT_IN = "..application..port..in..";
+    private static final String PORT_OUT = "..application..port..out..";
+    private static final String SERVICE = "..application..service..";
+    private static final String ADAPTER_IN = "..adapter..in..";
+    private static final String ADAPTER_OUT = "..adapter..out..";
+    private static final String ADAPTER_OUT_JPA = "..adapter..out.persistence.jpa..";
```

### 1475eba ArchitectureUnit 테스트 추가
- 변경 요약: ArchitectureUnit 테스트 추가
- 핵심 로직: 테스트 케이스 확장
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `coupon-service/src/test/java/com/example/couponservice/archunit/ArchitectureTest4CouponSercice.java`
- 코드 발췌: `coupon-service/src/test/java/com/example/couponservice/archunit/ArchitectureTest4CouponSercice.java`
```diff
+package com.example.couponservice.archunit;
+
+import com.tngtech.archunit.core.importer.ImportOption;
+import com.tngtech.archunit.junit.AnalyzeClasses;
+import com.tngtech.archunit.junit.ArchTest;
+import com.tngtech.archunit.lang.ArchRule;
+
+import static com.tngtech.archunit.base.DescribedPredicate.not;
```

### 6e8df39 Archunit 중복제거 리팩토링
- 변경 요약: Archunit 중복제거 리팩토링
- 핵심 로직: 아키텍처 규칙 테스트
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `common/build.gradle`, `common/src/testFixtures/java/com/example/common/archunit/HexagonalArchitectureRules.java`
- 코드 발췌: `common/build.gradle`
```diff
+    id 'java-test-fixtures'
+
+    testFixturesImplementation 'com.tngtech.archunit:archunit-junit5:1.3.0'
```
- 코드 발췌: `common/src/testFixtures/java/com/example/common/archunit/HexagonalArchitectureRules.java`
```diff
+package com.example.common.archunit;
+
+import com.tngtech.archunit.lang.ArchRule;
+
+import static com.tngtech.archunit.base.DescribedPredicate.not;
+import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
+import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
+import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
```

### e37883c ### Common 모듈 추가 ######################
- 변경 요약: ### Common 모듈 추가 ######################
- 핵심 로직: 모듈/의존성 구성 변경
- 구조 변화: 공통 모듈 추가 및 의존성 정리
- 주요 파일: `common/build.gradle`, `common/src/main/java/com/example/common/api/ApiError.java`
- 코드 발췌: `common/build.gradle`
```diff
+// org.springframework.boot 플러그인 절대 적용하지 않기
+plugins {
+    id 'java-library'
+}
+
+group = 'com.example'
+version = '0.0.1-SNAPSHOT'
```
- 코드 발췌: `common/src/main/java/com/example/common/api/ApiError.java`
```diff
+package com.example.common.api;
+
+// 공통 에러 DTO (web 계층)
+public class ApiError {
+    private final String code;
+    private final String message;
+    private ApiError(String code, String message) { this.code = code; this.message = message; }
+    public String getCode() { return code; }
```

### 868aa6f Archunit 검증 테스트 추가
- 변경 요약: Archunit 검증 테스트 추가
- 핵심 로직: 아키텍처 규칙 테스트
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `order-orchestrator/src/test/java/com/example/orderorchestrator/archunit/ArchitectureTest.java`
- 코드 발췌: `order-orchestrator/src/test/java/com/example/orderorchestrator/archunit/ArchitectureTest.java`
```diff
+        packages = "com.example.orderorchestrator",
+        importOptions = { ImportOption.DoNotIncludeTests.class }
+    private static final String PORT_IN = "..application..port..in..";
+    private static final String PORT_OUT = "..application..port..out..";
+    private static final String SERVICE = "..application..service..";
+    private static final String ADAPTER_IN = "..adapter..in..";
+    private static final String ADAPTER_OUT = "..adapter..out..";
+    private static final String ADAPTER_OUT_JPA = "..adapter..out.persistence.jpa..";
```

### 1475eba ArchitectureUnit 테스트 추가
- 변경 요약: ArchitectureUnit 테스트 추가
- 핵심 로직: 테스트 케이스 확장
- 구조 변화: 모듈/기능 추가로 책임 분리
- 주요 파일: `coupon-service/src/test/java/com/example/couponservice/archunit/ArchitectureTest4CouponSercice.java`
- 코드 발췌: `coupon-service/src/test/java/com/example/couponservice/archunit/ArchitectureTest4CouponSercice.java`
```diff
+package com.example.couponservice.archunit;
+
+import com.tngtech.archunit.core.importer.ImportOption;
+import com.tngtech.archunit.junit.AnalyzeClasses;
+import com.tngtech.archunit.junit.ArchTest;
+import com.tngtech.archunit.lang.ArchRule;
+
+import static com.tngtech.archunit.base.DescribedPredicate.not;
```

### 6e8df39 Archunit 중복제거 리팩토링
- 변경 요약: Archunit 중복제거 리팩토링
- 핵심 로직: 아키텍처 규칙 테스트
- 구조 변화: 오케스트레이터 책임/상태 관리 강화
- 주요 파일: `common/build.gradle`, `common/src/testFixtures/java/com/example/common/archunit/HexagonalArchitectureRules.java`
- 코드 발췌: `common/build.gradle`
```diff
+    id 'java-test-fixtures'
+
+    testFixturesImplementation 'com.tngtech.archunit:archunit-junit5:1.3.0'
```
- 코드 발췌: `common/src/testFixtures/java/com/example/common/archunit/HexagonalArchitectureRules.java`
```diff
+package com.example.common.archunit;
+
+import com.tngtech.archunit.lang.ArchRule;
+
+import static com.tngtech.archunit.base.DescribedPredicate.not;
+import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
+import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
+import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
```

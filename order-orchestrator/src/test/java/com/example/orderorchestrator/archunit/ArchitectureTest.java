package com.example.orderorchestrator.archunit;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(
        packages = "com.example.orderorchestrator",
        importOptions = { ImportOption.DoNotIncludeTests.class }
)
public class ArchitectureTest {

    private static final String DOMAIN = "..domain..";
    private static final String APPLICATION = "..application..";
    private static final String PORT_IN = "..application..port..in..";
    private static final String PORT_OUT = "..application..port..out..";
    private static final String SERVICE = "..application..service..";

    private static final String ADAPTER_IN = "..adapter..in..";
    private static final String ADAPTER_OUT = "..adapter..out..";
    private static final String ADAPTER_OUT_JPA = "..adapter..out.persistence.jpa..";
    // common 모듈은 모든 계층에서 의존 가능 (독립 계층)
    private static final String COMMON = "..common..";

    // =====================================================
    // 1. 도메인 패키지에 있는 어떤 클래스도
    //  APPLICATION / ADAPTER_IN / ADAPTER_OUT / 스프링 / JPA
    //  에 들어있는 클래스를 의존해서는 안 된다.
    // =====================================================
    @ArchTest
    static final ArchRule domain_should_not_depend_on_any_framework =
            noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            APPLICATION, ADAPTER_IN, ADAPTER_OUT,
                            "org.springframework..",
                            "jakarta.persistence.."
                    );

    // =====================================================
    // 2. Application 은 Domain, Port, Java 표준만 참조 가능
    // =====================================================
    @ArchTest
    static final ArchRule application_should_only_depend_on_domain_and_itself =
            classes().that().resideInAPackage(APPLICATION)
                    .should().onlyDependOnClassesThat().resideInAnyPackage(
                            APPLICATION,          // application.* (port, service 등 자기 계층)
                            DOMAIN,               // domain.*
                            COMMON,
                            "java..",
                            "jakarta..",
                            "javax..",
                            "org.springframework..",
                            "lombok.."            // @RequiredArgsConstructor 등
                    );

    // =====================================================
    // 3. Inbound Adapter(web)는 Port-In / Service / Domain 및 표준 라이브러리에만 의존 가능”
    // =====================================================
    @ArchTest
    static final ArchRule inbound_adapter_should_depend_on_port_in =
            classes().that().resideInAPackage(ADAPTER_IN)
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            ADAPTER_IN,              // 자기 패키지
                            PORT_IN,                 // Port-in
                            SERVICE,                 // optional: controller → service 직접 접근 가능 여부
                            DOMAIN,
                            COMMON,
                            "java..",
                            "jakarta..",
                            "javax..",               // ✅ javax.* 허용 (예: AccountNotFoundException)
                            "org.springframework..",
                            "lombok.."               // ✅ Lombok 애노테이션 허용
                    );

    // =====================================================
    // 4. Application 계층은 Adapter 계층에 의존하지 않는다 (반대 방향만 허용)
    // =====================================================
    @ArchTest
    static final ArchRule application_should_not_depend_on_adapters =
            noClasses().that().resideInAPackage(APPLICATION)
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(ADAPTER_IN, ADAPTER_OUT);


    // =====================================================
    // 5. Outbound Adapter(persistence)는
    //    - Port-Out, Domain, 표준 라이브러리, Spring 등에만 의존할 수 있고
    //    - 다른 Adapter 계층이나 Application 계층 구현체에는 의존하지 않는다.
    // =====================================================
    @ArchTest
    static final ArchRule outbound_adapter_should_only_depend_on_port_out_and_domain =
            classes().that().resideInAPackage(ADAPTER_OUT)
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            ADAPTER_OUT,              // 자기 계층
                            PORT_OUT,                 // port.out (반드시 이를 통해 도메인/외부와 연결)
                            DOMAIN,                   // 도메인 모델/상태
                            "java..",
                            "jakarta..",
                            "javax..",
                            "org.springframework..",
                            "lombok.."                // 필요하다면
                    );

    private static final String DOMAIN_MODEL    = "..domain..model..";
    private static final String DOMAIN_STATUS   = "..domain..model..status..";

    // =====================================================
    // 6. JPA 엔티티는 도메인 엔티티를 참조하면 안 된다
    // =====================================================
    /**
     * JPA 엔티티가 Domain Model(엔터티/값객체 등) 에 직접 의존하지 않도록 강제하는 규칙.
     *
     * 헥사고날 아키텍처(Ports & Adapters)에서는 Persistence Layer(JPA)가
     * 도메인의 내부 모델(domain.model.*)을 직접 참조하는 것이 금지된다.
     * 그래야 도메인 로직이 인프라(JPA)에 오염되지 않고,
     * 또한 persistence 구현체 교체 시(예: JPA → R2DBC → Mongo) 도메인이 안전하게 유지된다.
     *
     * 단, domain.model.status.* 패키지의 Enum(MSAStatus, OrderSagaStatus)은 예외로 허용한다.
     * 이 상태 값들은 도메인의 공통 언어(Ubiquitous Language)이자 스키마와 1:1 매핑되는 값으로서,
     * JPA 엔티티에서 상태 필드로 참조하는 것이 구조적으로 자연스럽기 때문이다.
     *
     * 요약:
     *   - 금지: JPA → domain.model.*, domain.model.saga.*, domain.model.order.* 등
     *   - 허용: JPA → domain.model.status.* (MSAStatus, OrderSagaStatus)
     *
     * 결과적으로:
     *   - 도메인 엔티티/VO 구조는 JPA 엔티티로부터 완전히 보호되고,
     *   - 상태 enum은 persistence 와 domain 양쪽에서 공통으로 사용할 수 있다.
     */
    @ArchTest
    static final ArchRule jpa_entities_should_not_depend_on_domain_model_except_status =
            noClasses().that().resideInAPackage(ADAPTER_OUT_JPA)
                    .should().dependOnClassesThat(
                            resideInAnyPackage(DOMAIN_MODEL)                  // domain.model.*
                                    .and(not(resideInAnyPackage(DOMAIN_STATUS))) // 단, status 패키지는 예외
                    );


    // =====================================================
    // 7. Domain ↔ Adapter 직접 참조 금지
    // =====================================================
    @ArchTest
    static final ArchRule domain_should_not_depend_on_adapter =
            noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(ADAPTER_IN, ADAPTER_OUT);


    // =====================================================
    // 8. 순환 의존 금지
    // =====================================================
    @ArchTest
    static final ArchRule no_cycles =
            slices().matching("com.example.orderorchestrator.(*)..")
                    .should().beFreeOfCycles();
}

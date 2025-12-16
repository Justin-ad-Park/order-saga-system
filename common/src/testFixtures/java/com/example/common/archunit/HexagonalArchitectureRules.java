package com.example.common.archunit;

import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * 공유 가능한 헥사고날 아키텍처 ArchUnit 룰 모음.
 * 서비스별 베이스 패키지만 넘기면 동일한 규칙을 재사용할 수 있다.
 */
public final class HexagonalArchitectureRules {

    private static final String DOMAIN = "..domain..";
    private static final String APPLICATION = "..application..";
    private static final String PORT_IN = "..application..port..in..";
    private static final String PORT_OUT = "..application..port..out..";
    private static final String SERVICE = "..application..service..";

    private static final String ADAPTER_IN = "..adapter..in..";
    private static final String ADAPTER_OUT = "..adapter..out..";
    private static final String ADAPTER_OUT_JPA = "..adapter..out.persistence.jpa..";
    private static final String COMMON = "..common..";

    private static final String DOMAIN_MODEL = "..domain..model..";
    private static final String DOMAIN_STATUS = "..domain..model..status..";

    private final String basePackage;

    private HexagonalArchitectureRules(String basePackage) {
        this.basePackage = basePackage;
    }

    public static HexagonalArchitectureRules getInstance(String basePackage) {
        return new HexagonalArchitectureRules(basePackage);
    }

    public ArchRule domainShouldNotDependOnAnyFramework() {
        return noClasses().that().resideInAPackage(DOMAIN)
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        APPLICATION, ADAPTER_IN, ADAPTER_OUT,
                        "org.springframework..",
                        "jakarta.persistence.."
                );
    }

    public ArchRule applicationShouldOnlyDependOnDomainAndItself() {
        return classes().that().resideInAPackage(APPLICATION)
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        APPLICATION,
                        DOMAIN,
                        COMMON,
                        "java..",
                        "jakarta..",
                        "javax..",
                        "org.springframework..",
                        "lombok.."
                );
    }

    public ArchRule inboundAdapterShouldDependOnPortIn() {
        return classes().that().resideInAPackage(ADAPTER_IN)
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        ADAPTER_IN,
                        PORT_IN,
                        SERVICE,
                        DOMAIN,
                        COMMON,
                        "java..",
                        "jakarta..",
                        "javax..",
                        "org.springframework..",
                        "lombok.."
                );
    }

    public ArchRule applicationShouldNotDependOnAdapters() {
        return noClasses().that().resideInAPackage(APPLICATION)
                .should().dependOnClassesThat()
                .resideInAnyPackage(ADAPTER_IN, ADAPTER_OUT);
    }

    public ArchRule outboundAdapterShouldOnlyDependOnPortOutAndDomain() {
        return classes().that().resideInAPackage(ADAPTER_OUT)
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        ADAPTER_OUT,
                        PORT_OUT,
                        DOMAIN,
                        "java..",
                        "jakarta..",
                        "javax..",
                        "org.springframework..",
                        "lombok.."
                );
    }

    public ArchRule jpaEntitiesShouldNotDependOnDomainModelExceptStatus() {
        return noClasses().that().resideInAPackage(ADAPTER_OUT_JPA)
                .should().dependOnClassesThat(
                        resideInAnyPackage(DOMAIN_MODEL)
                                .and(not(resideInAnyPackage(DOMAIN_STATUS)))
                );
    }

    public ArchRule domainShouldNotDependOnAdapter() {
        return noClasses().that().resideInAPackage(DOMAIN)
                .should().dependOnClassesThat()
                .resideInAnyPackage(ADAPTER_IN, ADAPTER_OUT);
    }

    public ArchRule noCycles() {
        return slices().matching(basePackage + ".(*)..")
                .should().beFreeOfCycles();
    }
}

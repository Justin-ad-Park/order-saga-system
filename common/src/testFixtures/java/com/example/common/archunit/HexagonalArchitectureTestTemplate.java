package com.example.common.archunit;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.core.domain.JavaClasses;

/**
 * 베이스 패키지만 제공하면 공통 ArchUnit 룰을 실행하는 템플릿.
 * 구체 테스트 클래스는 BASE_PACKAGE 상수와 basePackage()만 구현하면 된다.
 */
public abstract class HexagonalArchitectureTestTemplate {

    protected abstract String basePackage();

    private HexagonalArchitectureRules rules() {
        return HexagonalArchitectureRules.getInstance(basePackage());
    }

    @ArchTest
    void domain_should_not_depend_on_any_framework(JavaClasses importedClasses) {
        rules().domainShouldNotDependOnAnyFramework().check(importedClasses);
    }

    @ArchTest
    void application_should_only_depend_on_domain_and_itself(JavaClasses importedClasses) {
        rules().applicationShouldOnlyDependOnDomainAndItself().check(importedClasses);
    }

    @ArchTest
    void inbound_adapter_should_depend_on_port_in(JavaClasses importedClasses) {
        rules().inboundAdapterShouldDependOnPortIn().check(importedClasses);
    }

    @ArchTest
    void application_should_not_depend_on_adapters(JavaClasses importedClasses) {
        rules().applicationShouldNotDependOnAdapters().check(importedClasses);
    }

    @ArchTest
    void outbound_adapter_should_only_depend_on_port_out_and_domain(JavaClasses importedClasses) {
        rules().outboundAdapterShouldOnlyDependOnPortOutAndDomain().check(importedClasses);
    }

    @ArchTest
    void jpa_entities_should_not_depend_on_domain_model_except_status(JavaClasses importedClasses) {
        rules().jpaEntitiesShouldNotDependOnDomainModelExceptStatus().check(importedClasses);
    }

    @ArchTest
    void domain_should_not_depend_on_adapter(JavaClasses importedClasses) {
        rules().domainShouldNotDependOnAdapter().check(importedClasses);
    }

    @ArchTest
    void no_cycles(JavaClasses importedClasses) {
        rules().noCycles().check(importedClasses);
    }
}

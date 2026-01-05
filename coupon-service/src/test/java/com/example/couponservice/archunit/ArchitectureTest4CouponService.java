package com.example.couponservice.archunit;

import com.example.common.archunit.HexagonalArchitectureTestTemplate;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;

@AnalyzeClasses(
        packages = ArchitectureTest4CouponService.BASE_PACKAGE,
        importOptions = { ImportOption.DoNotIncludeTests.class }
)
public class ArchitectureTest4CouponService extends HexagonalArchitectureTestTemplate {

    static final String BASE_PACKAGE = "com.example.couponservice";

    @Override
    protected String basePackage() {
        return BASE_PACKAGE;
    }
}

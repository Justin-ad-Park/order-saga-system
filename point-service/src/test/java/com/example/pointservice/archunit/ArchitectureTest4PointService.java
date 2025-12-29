package com.example.pointservice.archunit;

import com.example.common.archunit.HexagonalArchitectureTestTemplate;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;

@AnalyzeClasses(
        packages = ArchitectureTest4PointService.BASE_PACKAGE,
        importOptions = { ImportOption.DoNotIncludeTests.class }
)
public class ArchitectureTest4PointService extends HexagonalArchitectureTestTemplate {

    static final String BASE_PACKAGE = "com.example.pointservice";

    @Override
    protected String basePackage() {
        return BASE_PACKAGE;
    }
}

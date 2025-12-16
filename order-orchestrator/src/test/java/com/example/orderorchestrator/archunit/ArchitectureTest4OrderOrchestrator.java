package com.example.orderorchestrator.archunit;

import com.example.common.archunit.HexagonalArchitectureTestTemplate;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;

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

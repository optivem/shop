package com.mycompany.myshop.backend.infrastructure.config;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionConfigGuardTest {

    @Test
    void rejectsTheStubClockButAcceptsRealMode() {
        assertThat(ProductionConfigGuard.problems(key -> "external.system-mode".equals(key) ? "stub" : null))
            .contains("EXTERNAL_SYSTEM_MODE is not real (prod reads the local clock; the stub clock is for test and pipeline stacks)");
        assertThat(ProductionConfigGuard.problems(key -> "external.system-mode".equals(key) ? "real" : null))
            .noneMatch(problem -> problem.startsWith("EXTERNAL_SYSTEM_MODE"));
    }

    @Test
    void acceptsRealValues() {
        Map<String, String> config = Map.of("spring.datasource.password", "s3cret");

        assertThat(ProductionConfigGuard.problems(config::get)).isEmpty();
    }

    @Test
    void rejectsTestDefaults() {
        Map<String, String> config = Map.of("spring.datasource.password", "app");

        assertThat(ProductionConfigGuard.problems(config::get)).containsExactly("POSTGRES_DB_PASSWORD is missing or the test default");
    }

    @Test
    void rejectsMissingValues() {
        assertThat(ProductionConfigGuard.problems(key -> null)).hasSize(1);
    }
}

package com.mycompany.myshop.config;

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
        Map<String, String> config = Map.of("spring.datasource.password", "s3cret", "auth.issuer-uri", "https://auth.example.com/realms/shop", "auth.client-secret", "real-secret");

        assertThat(ProductionConfigGuard.problems(config::get)).isEmpty();
    }

    @Test
    void rejectsTestDefaults() {
        Map<String, String> config = Map.of("spring.datasource.password", "app", "auth.issuer-uri", "http://localhost:8191/realms/shop", "auth.client-secret", "shop-monolith-test-secret");

        assertThat(ProductionConfigGuard.problems(config::get)).containsExactly("POSTGRES_DB_PASSWORD is missing or the test default", "AUTH_ISSUER_URI is missing or points at localhost", "AUTH_CLIENT_SECRET is missing or the test-realm secret");
    }

    @Test
    void rejectsMissingValues() {
        assertThat(ProductionConfigGuard.problems(key -> null)).hasSize(3);
    }
}

package com.mycompany.myshop.backend.config;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionConfigGuardTest {

    @Test
    void acceptsRealValues() {
        Map<String, String> config = Map.of("spring.datasource.password", "s3cret", "auth.issuer-uri", "https://auth.example.com/realms/shop");

        assertThat(ProductionConfigGuard.problems(config::get)).isEmpty();
    }

    @Test
    void rejectsTestDefaults() {
        Map<String, String> config = Map.of("spring.datasource.password", "app", "auth.issuer-uri", "http://localhost:8191/realms/shop");

        assertThat(ProductionConfigGuard.problems(config::get)).containsExactly("POSTGRES_DB_PASSWORD is missing or the test default", "AUTH_ISSUER_URI is missing or points at localhost");
    }

    @Test
    void rejectsMissingValues() {
        assertThat(ProductionConfigGuard.problems(key -> null)).hasSize(2);
    }
}

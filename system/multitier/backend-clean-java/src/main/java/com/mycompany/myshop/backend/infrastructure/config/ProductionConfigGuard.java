package com.mycompany.myshop.backend.infrastructure.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

// Fails startup under the prod profile when the DB password is missing or the committed test default.
@Component
@Profile("prod")
public class ProductionConfigGuard {

    static final String REAL_MODE = "real";
    static final String TEST_DB_PASSWORD = "app";


    public ProductionConfigGuard(Environment environment) {
        var problems = problems(environment::getProperty);
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Refusing to start with the prod profile: " + String.join("; ", problems));
        }
    }

    static List<String> problems(UnaryOperator<String> property) {
        var problems = new ArrayList<String>();
        var dbPassword = property.apply("spring.datasource.password");
        if (isBlank(dbPassword) || TEST_DB_PASSWORD.equals(dbPassword)) {
            problems.add("POSTGRES_DB_PASSWORD is missing or the test default");
        }
        var externalSystemMode = property.apply("external.system-mode");
        if (externalSystemMode != null && !REAL_MODE.equals(externalSystemMode)) {
            problems.add("EXTERNAL_SYSTEM_MODE is not real (prod reads the local clock; the stub clock is for test and pipeline stacks)");
        }
        return problems;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

package com.mycompany.myshop.backend.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fails startup under the prod profile when a secret or endpoint is missing or still the committed test
 * default (DB password {@code app}, a localhost issuer). Local and pipeline stacks run without the
 * profile, so their test defaults keep working.
 */
@Component
@Profile("prod")
public class ProductionConfigGuard {

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
        var issuer = property.apply("auth.issuer-uri");
        if (isBlank(issuer) || isLocalhost(issuer)) {
            problems.add("AUTH_ISSUER_URI is missing or points at localhost");
        }
        return problems;
    }

    static boolean isLocalhost(String uri) {
        try {
            var host = URI.create(uri).getHost();
            return host == null || host.equals("localhost") || host.equals("127.0.0.1");
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

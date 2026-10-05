package com.mycompany.myshop.backend.backendtest.configuration;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Replaces the JWKS-backed decoder with one that accepts two opaque bearer tokens, so the real
 * security filter chain (matchers, role mapping, error bodies) runs without an identity provider.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestAuthConfiguration {

    public static final String ADMIN_TOKEN = "admin-token";
    public static final String CUSTOMER_TOKEN = "customer-token";
    public static final String OTHER_CUSTOMER_TOKEN = "other-customer-token";

    @Bean
    @Primary
    public JwtDecoder testJwtDecoder() {
        return token -> switch (token) {
            case ADMIN_TOKEN -> jwtWithRoles(token, "admin-user", "ADMIN");
            case CUSTOMER_TOKEN -> jwtWithRoles(token, "customer-user", "CUSTOMER");
            case OTHER_CUSTOMER_TOKEN -> jwtWithRoles(token, "other-customer-user", "CUSTOMER");
            default -> throw new BadJwtException("Unknown test token");
        };
    }

    private static Jwt jwtWithRoles(String token, String subject, String role) {
        var now = Instant.now();
        return Jwt.withTokenValue(token)
            .header("alg", "none")
            .subject(subject)
            .claim("preferred_username", subject)
            .claim("realm_access", Map.of("roles", List.of(role)))
            .issuedAt(now)
            .expiresAt(now.plusSeconds(3600))
            .build();
    }
}

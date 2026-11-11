package com.mycompany.myshop.backend.core.services.external;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// Bound from resilience.* in application.yml and shared by every outbound gateway. The settings are
// shared, the state is not: each gateway builds its own GatewayHttpClient, so each external system
// gets its own circuit breaker and a failing ERP cannot open the tax service's breaker.
//
// DEFAULTS mirrors application.yml for code that builds a gateway outside a Spring context (the
// narrow-integration tests); keep the two in step.
@ConfigurationProperties(prefix = "resilience")
public record ResilienceSettings(
        Duration connectTimeout,
        Duration readTimeout,
        RetrySettings retry,
        CircuitBreakerSettings circuitBreaker) {

    public static final ResilienceSettings DEFAULTS = new ResilienceSettings(
            Duration.ofSeconds(1),
            Duration.ofSeconds(2),
            new RetrySettings(3, Duration.ofMillis(200), 2.0, 0.5),
            new CircuitBreakerSettings(10, 5, 50.0f, Duration.ofSeconds(10), 2));

    // maxAttempts counts the first call: 3 means one call and at most two retries.
    public record RetrySettings(
            int maxAttempts,
            Duration initialBackoff,
            double backoffMultiplier,
            double jitter) {
    }

    public record CircuitBreakerSettings(
            int slidingWindowSize,
            int minimumNumberOfCalls,
            float failureRateThreshold,
            Duration waitInOpenState,
            int permittedCallsInHalfOpenState) {
    }
}

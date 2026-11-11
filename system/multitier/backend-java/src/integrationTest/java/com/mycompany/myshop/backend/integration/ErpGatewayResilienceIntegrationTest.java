package com.mycompany.myshop.backend.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.mycompany.myshop.backend.core.exceptions.ErpUnavailableException;
import com.mycompany.myshop.backend.core.services.external.ErpGateway;
import com.mycompany.myshop.backend.core.services.external.ResilienceSettings;
import com.mycompany.myshop.backend.core.services.external.ResilienceSettings.CircuitBreakerSettings;
import com.mycompany.myshop.backend.core.services.external.ResilienceSettings.RetrySettings;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The resilience policy on the ERP gateway (GatewayHttpClient): retry on a transient failure, no
 * retry on a 4xx, a read timeout, and a circuit breaker that fails fast once the ERP is down. Raw
 * WireMock with no latest/legacy twin: the scenarios (a stub that fails then recovers, a slow stub)
 * are about the transport, not the ERP's contract, so the shared DSL has no step for them.
 *
 * <p>Backoff is shrunk from the production 200ms and the timeout test uses a 200ms read timeout,
 * so the class runs in about a second; attempt counts and breaker thresholds are the production ones.
 */
class ErpGatewayResilienceIntegrationTest {

    private static final WireMockServer WIRE_MOCK = new WireMockServer(options().dynamicPort());

    // Production's read timeout everywhere except the timeout test, so a cold first request (JIT,
    // WireMock warm-up) is never mistaken for a slow ERP.
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration SHORT_READ_TIMEOUT = Duration.ofMillis(200);

    @BeforeAll
    static void startWireMock() {
        WIRE_MOCK.start();
    }

    @AfterAll
    static void stopWireMock() {
        WIRE_MOCK.stop();
    }

    @BeforeEach
    void resetWireMock() {
        WIRE_MOCK.resetAll();
    }

    @Test
    void retriesATransientServerErrorAndThenSucceeds() {
        WIRE_MOCK.stubFor(get("/api/products/BOOK-123").inScenario("flaky")
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("recovered"));
        WIRE_MOCK.stubFor(get("/api/products/BOOK-123").inScenario("flaky")
            .whenScenarioStateIs("recovered")
            .willReturn(okJson("{\"id\":\"BOOK-123\",\"price\":10.00}")));

        var result = gateway(3).getProductDetails("BOOK-123");

        assertThat(result).isPresent();
        WIRE_MOCK.verify(2, getRequestedFor(urlEqualTo("/api/products/BOOK-123")));
    }

    @Test
    void givesUpAfterThreeAttempts() {
        WIRE_MOCK.stubFor(get("/api/products/BOOK-123").willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> gateway(3).getProductDetails("BOOK-123"))
            .isInstanceOf(ErpUnavailableException.class)
            .hasMessageContaining("503");
        WIRE_MOCK.verify(3, getRequestedFor(urlEqualTo("/api/products/BOOK-123")));
    }

    @Test
    void doesNotRetryANotFound() {
        WIRE_MOCK.stubFor(get("/api/products/UNKNOWN").willReturn(aResponse().withStatus(404)));

        assertThat(gateway(3).getProductDetails("UNKNOWN")).isEmpty();
        WIRE_MOCK.verify(1, getRequestedFor(urlEqualTo("/api/products/UNKNOWN")));
    }

    @Test
    void timesOutASlowErp() {
        WIRE_MOCK.stubFor(get("/api/products/BOOK-123")
            .willReturn(okJson("{\"id\":\"BOOK-123\",\"price\":10.00}").withFixedDelay(1_000)));

        assertThatThrownBy(() -> gateway(1, SHORT_READ_TIMEOUT).getProductDetails("BOOK-123"))
            .isInstanceOf(ErpUnavailableException.class)
            .hasMessageContaining("HttpTimeoutException");
    }

    @Test
    void opensTheBreakerAndThenFailsFastWithoutCallingTheErp() {
        WIRE_MOCK.stubFor(get("/api/products/BOOK-123").willReturn(aResponse().withStatus(500)));
        var erpGateway = gateway(1);

        // Minimum number of calls is 5; all five fail, so the failure rate is 100% > 50%.
        for (var i = 0; i < 5; i++) {
            assertThatThrownBy(() -> erpGateway.getProductDetails("BOOK-123"))
                .isInstanceOf(ErpUnavailableException.class)
                .hasMessageContaining("500");
        }

        assertThatThrownBy(() -> erpGateway.getProductDetails("BOOK-123"))
            .isInstanceOf(ErpUnavailableException.class)
            .hasMessageContaining("Circuit breaker for erp is open");
        WIRE_MOCK.verify(5, getRequestedFor(urlEqualTo("/api/products/BOOK-123")));
    }

    private static ErpGateway gateway(int maxAttempts) {
        return gateway(maxAttempts, READ_TIMEOUT);
    }

    private static ErpGateway gateway(int maxAttempts, Duration readTimeout) {
        var settings = new ResilienceSettings(
            Duration.ofMillis(500),
            readTimeout,
            new RetrySettings(maxAttempts, Duration.ofMillis(10), 2.0, 0.5),
            new CircuitBreakerSettings(10, 5, 50.0f, Duration.ofSeconds(10), 2));
        return new ErpGateway(WIRE_MOCK.baseUrl(), settings);
    }
}

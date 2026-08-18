package com.mycompany.myshop.backend.integration.legacy;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.mycompany.myshop.backend.domain.gateways.ClockGateway;
import com.mycompany.myshop.backend.infrastructure.external.ClockGatewayException;
import com.mycompany.myshop.backend.infrastructure.external.clock.HttpClockGateway;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClockGatewayIntegrationTest {

    static final WireMockServer WIRE_MOCK = new WireMockServer(options().dynamicPort());

    @BeforeAll
    static void startWireMock() {
        WIRE_MOCK.start();
    }

    @AfterAll
    static void stopWireMock() {
        WIRE_MOCK.stop();
    }

    @BeforeEach
    void setUp() {
        WIRE_MOCK.resetAll();
    }

    private ClockGateway clockGateway(String rawMode) {
        return new HttpClockGateway(rawMode, WIRE_MOCK.baseUrl());
    }

    @Test
    void getCurrentTimeReturnsStubbedTimeInStubMode() {
        WIRE_MOCK.stubFor(get("/api/time")
            .willReturn(okJson("{\"time\":\"2026-03-10T12:00:00Z\"}")));

        assertThat(clockGateway("stub").getCurrentTime())
            .isEqualTo(Instant.parse("2026-03-10T12:00:00Z"));
    }

    @Test
    void getCurrentTimeThrowsOnServerError() {
        WIRE_MOCK.stubFor(get("/api/time")
            .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

        assertThatThrownBy(() -> clockGateway("stub").getCurrentTime())
            .isInstanceOf(ClockGatewayException.class)
            .hasMessageContaining("500");
    }

    @Test
    void getCurrentTimeIgnoresStubInRealMode() {
        WIRE_MOCK.stubFor(get("/api/time")
            .willReturn(okJson("{\"time\":\"2026-03-10T12:00:00Z\"}")));

        // The stub is programmed but must not be consulted: real mode reads the system clock.
        assertThat(clockGateway("real").getCurrentTime())
            .isCloseTo(Instant.now(), within(10, ChronoUnit.SECONDS));
    }

    @Test
    void getCurrentTimeRejectsUnknownMode() {
        assertThatThrownBy(() -> clockGateway("bogus").getCurrentTime())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("bogus");
    }
}

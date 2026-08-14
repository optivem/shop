package com.mycompany.myshop.backend.integration.legacy;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.mycompany.myshop.backend.core.services.external.ClockGateway;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * "Before" of the external-systems contract-tests refactor at the narrow-integration layer: the
 * {@link ClockGateway} exercised against Clock stubbed by raw, inlined WireMock. The {@code latest/}
 * twin drives the identical four scenarios through the shared stub DSL.
 *
 * <p>The WireMock lifecycle below is copied verbatim from {@code legacy/ErpGatewayIntegrationTest}
 * and again in {@code legacy/TaxGatewayIntegrationTest} — see the note there. What this file adds is
 * the {@code external.system-mode} branch, which is the sharper half of the contrast: the mode is a
 * {@code @Value} string on the gateway, so a raw test has no way to set it other than reflectively,
 * by the field's name, with the mode's wire value spelled as a string literal. Both {@code "real"}
 * and {@code "bogus"} below are load-bearing strings that nothing checks at compile time.
 *
 * <p>The {@code latest/} twin closes half that gap and deliberately leaves the other half open: it
 * passes {@code ExternalSystemMode.REAL} through {@code clockGateway(mode)}, so the supported modes
 * are typed, but keeps {@code clockGatewayWithRawMode("bogus")} taking a raw string — typing that
 * argument would make the SUT's unknown-mode branch unreachable from a test. The reflective
 * {@code setField} does not disappear either; it moves into {@code Gateways}, so a rename of
 * {@code clockUrl} breaks one call site instead of three.
 */
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

    /**
     * Built per test rather than in {@code setUp}: unlike the ERP and Tax twins, the mode varies
     * across the scenarios below, and it is fixed at construction.
     */
    private ClockGateway clockGateway(String rawMode) {
        return new ClockGateway(rawMode, WIRE_MOCK.baseUrl());
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
            .isInstanceOf(IllegalStateException.class)
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

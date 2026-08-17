package com.mycompany.myshop.backend.integration.legacy;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.mycompany.myshop.backend.domain.values.Country;
import com.mycompany.myshop.backend.domain.exceptions.TaxGatewayException;
import com.mycompany.myshop.backend.domain.gateways.TaxGateway;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.infrastructure.external.tax.HttpTaxGateway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * "Before" of the external-systems contract-tests refactor at the narrow-integration layer: the
 * {@link TaxGateway} exercised against Tax stubbed by raw, inlined WireMock. The {@code latest/}
 * twin drives the identical four scenarios through the shared stub DSL.
 *
 * <p>Read this alongside {@code legacy/ErpGatewayIntegrationTest}: the WireMock lifecycle below —
 * static server, {@code @BeforeAll} start, {@code @AfterAll} stop, {@code resetAll} per test, plus
 * the hand-wired gateway URL — is copied verbatim from it, and copied again in
 * {@code legacy/ClockGatewayIntegrationTest}. Three near-identical harnesses is the duplication the
 * {@code latest/} twins delete by inheriting {@code BaseGatewayIntegrationTest}, and it is the
 * clearest thing this file is here to show.
 *
 * <p>The second thing it shows is specific to Tax: the 404 and 5xx stubs are three lines of raw
 * status-code plumbing each, so the distinction that actually matters — 404 means "no such country"
 * and yields an empty {@code Optional}, any other non-200 means Tax failed to answer and yields a
 * {@link TaxGatewayException} — has to be reconstructed by the reader from HTTP codes. The
 * {@code latest/} twin names it: {@code returnsNoTaxRate()} versus {@code failsForCountry()}.
 */
class TaxGatewayIntegrationTest {

    static final WireMockServer WIRE_MOCK = new WireMockServer(options().dynamicPort());

    private TaxGateway taxGateway;

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

        taxGateway = new HttpTaxGateway(WIRE_MOCK.baseUrl());
    }

    @Test
    void getTaxDetailsReturnsRateWhenCountryKnown() {
        WIRE_MOCK.stubFor(get("/api/countries/US")
            .willReturn(okJson("{\"id\":\"US\",\"countryName\":\"US\",\"taxRate\":0.10}")));

        var result = taxGateway.getTaxDetails(Country.of("US"));

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("US");
        assertThat(result.get().getRate()).isEqualTo(Rate.of("0.10"));
    }

    @Test
    void getTaxDetailsReturnsEmptyWhenCountryUnknown() {
        WIRE_MOCK.stubFor(get("/api/countries/ZZ")
            .willReturn(aResponse().withStatus(404)));

        assertThat(taxGateway.getTaxDetails(Country.of("ZZ"))).isEmpty();
    }

    @Test
    void getTaxDetailsThrowsOnServerError() {
        WIRE_MOCK.stubFor(get("/api/countries/US")
            .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

        assertThatThrownBy(() -> taxGateway.getTaxDetails(Country.of("US")))
            .isInstanceOf(TaxGatewayException.class)
            .hasMessageContaining("500");
    }

    @Test
    void getTaxDetailsThrowsOnServiceUnavailable() {
        WIRE_MOCK.stubFor(get("/api/countries/US")
            .willReturn(aResponse().withStatus(503).withBody("Service Unavailable")));

        assertThatThrownBy(() -> taxGateway.getTaxDetails(Country.of("US")))
            .isInstanceOf(TaxGatewayException.class)
            .hasMessageContaining("503");
    }
}

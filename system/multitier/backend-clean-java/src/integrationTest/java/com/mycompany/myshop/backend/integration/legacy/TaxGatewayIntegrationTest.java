package com.mycompany.myshop.backend.integration.legacy;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.mycompany.myshop.backend.domain.values.Country;
import com.mycompany.myshop.backend.infrastructure.external.TaxGatewayException;
import com.mycompany.myshop.backend.domain.gateways.TaxGateway;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.infrastructure.external.tax.HttpTaxGateway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        assertThat(result.get().getCountryName()).isEqualTo(Country.of("US"));
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

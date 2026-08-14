package com.mycompany.myshop.backend.integration.legacy;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.mycompany.myshop.backend.core.services.external.ErpGateway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * "Before" of the external-systems contract-tests refactor at the narrow-integration layer: the
 * {@link ErpGateway} exercised against ERP stubbed by raw, inlined WireMock. Uses an in-process
 * {@link WireMockServer} (same mechanism as the component tests) rather than a Testcontainers
 * WireMock container, so the layer needs no Docker. The {@code latest/} twin drives the happy/404
 * shapes through the shared stub DSL; the 500/503 error-injection cases stay raw in both twins.
 */
class ErpGatewayIntegrationTest {

    static final WireMockServer WIRE_MOCK = new WireMockServer(options().dynamicPort());

    private ErpGateway erpGateway;

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

        erpGateway = new ErpGateway(WIRE_MOCK.baseUrl());
    }

    @Test
    void getProductDetailsReturnsDetailsWhenFound() {
        WIRE_MOCK.stubFor(get("/api/products/BOOK-123")
            .willReturn(okJson("{\"id\":\"BOOK-123\",\"price\":10.00}")));

        var result = erpGateway.getProductDetails("BOOK-123");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("BOOK-123");
        assertThat(result.get().getPrice()).isEqualByComparingTo("10.00");
    }

    @Test
    void getProductDetailsReturnsEmptyWhenNotFound() {
        WIRE_MOCK.stubFor(get("/api/products/UNKNOWN")
            .willReturn(aResponse().withStatus(404)));

        assertThat(erpGateway.getProductDetails("UNKNOWN")).isEmpty();
    }

    @Test
    void getProductDetailsThrowsOnServerError() {
        WIRE_MOCK.stubFor(get("/api/products/BAD-SKU")
            .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

        assertThatThrownBy(() -> erpGateway.getProductDetails("BAD-SKU"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("500");
    }

    @Test
    void getPromotionDetailsReturnsPromotion() {
        WIRE_MOCK.stubFor(get("/api/promotion")
            .willReturn(okJson("{\"promotionActive\":true,\"discount\":0.15}")));

        var result = erpGateway.getPromotionDetails();

        assertThat(result.isPromotionActive()).isTrue();
        assertThat(result.getDiscount()).isEqualByComparingTo("0.15");
    }

    @Test
    void getPromotionDetailsThrowsOnServerError() {
        WIRE_MOCK.stubFor(get("/api/promotion")
            .willReturn(aResponse().withStatus(503).withBody("Service Unavailable")));

        assertThatThrownBy(() -> erpGateway.getPromotionDetails())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("503");
    }
}

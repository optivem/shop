package com.mycompany.myshop.backend.integration.latest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.mycompany.myshop.backend.core.services.external.ErpGateway;
import com.mycompany.myshop.backend.support.harness.ErpStubDriver;
import com.mycompany.myshop.backend.support.core.usecase.external.erp.ErpDsl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * "After" of the external-systems contract-tests refactor at the narrow-integration layer: identical
 * scenarios to the {@code legacy/} twin, but the ERP happy/404 stubs are declared through the shared
 * use case DSL under {@code support/} — the same {@link ErpDsl} the component {@code latest/} tests
 * reach as {@code app.erp()}. A narrow-integration test drives one gateway, not a scenario, so it
 * uses the use case layer directly and never sees the scenario DSL above it. Uses the same in-process
 * {@link WireMockServer} mechanism (no Docker). The 500/503 error-injection cases have no DSL
 * vocabulary and stay raw, matching the {@code legacy/} twin.
 */
class ErpGatewayIntegrationTest {

    static final WireMockServer WIRE_MOCK = new WireMockServer(options().dynamicPort());

    private ErpDsl erp;
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

        erp = new ErpDsl(new ErpStubDriver(new WireMock("localhost", WIRE_MOCK.port())));

        erpGateway = new ErpGateway();
        ReflectionTestUtils.setField(erpGateway, "erpUrl", WIRE_MOCK.baseUrl());
    }

    @Test
    void getProductDetailsReturnsDetailsWhenFound() {
        erp.returnsProduct().sku("BOOK-123").unitPrice("10.00").execute();

        var result = erpGateway.getProductDetails("BOOK-123");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("BOOK-123");
        assertThat(result.get().getPrice()).isEqualByComparingTo("10.00");
    }

    @Test
    void getProductDetailsReturnsEmptyWhenNotFound() {
        erp.returnsNoProduct().sku("UNKNOWN").execute();

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
        erp.returnsPromotion().active(true).discount("0.15").execute();

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

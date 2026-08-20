package com.mycompany.myshop.backend.integration.legacy;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.mycompany.myshop.backend.domain.gateways.ErpGateway;
import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.domain.values.Sku;
import com.mycompany.myshop.backend.domain.gateways.ErpGatewayException;
import com.mycompany.myshop.backend.infrastructure.external.erp.HttpErpGateway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

        erpGateway = new HttpErpGateway(WIRE_MOCK.baseUrl());
    }

    @Test
    void getProductDetailsReturnsDetailsWhenFound() {
        WIRE_MOCK.stubFor(get("/api/products/BOOK-123")
            .willReturn(okJson("{\"id\":\"BOOK-123\",\"price\":10.00}")));

        var result = erpGateway.getProductDetails(Sku.of("BOOK-123"));

        assertThat(result).isPresent();
        assertThat(result.get().sku()).isEqualTo(Sku.of("BOOK-123"));
        assertThat(result.get().price()).isEqualTo(Money.of("10.00"));
    }

    @Test
    void getProductDetailsReturnsEmptyWhenNotFound() {
        WIRE_MOCK.stubFor(get("/api/products/UNKNOWN")
            .willReturn(aResponse().withStatus(404)));

        assertThat(erpGateway.getProductDetails(Sku.of("UNKNOWN"))).isEmpty();
    }

    @Test
    void getProductDetailsThrowsOnServerError() {
        WIRE_MOCK.stubFor(get("/api/products/BAD-SKU")
            .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

        assertThatThrownBy(() -> erpGateway.getProductDetails(Sku.of("BAD-SKU")))
            .isInstanceOf(ErpGatewayException.class)
            .hasMessageContaining("500");
    }

    @Test
    void getPromotionDetailsReturnsPromotion() {
        WIRE_MOCK.stubFor(get("/api/promotion")
            .willReturn(okJson("{\"promotionActive\":true,\"discount\":0.15}")));

        var result = erpGateway.getPromotionDetails();

        assertThat(result.active()).isTrue();
        assertThat(result.discount()).isEqualTo(Rate.of("0.15"));
    }

    @Test
    void getPromotionDetailsThrowsOnServerError() {
        WIRE_MOCK.stubFor(get("/api/promotion")
            .willReturn(aResponse().withStatus(503).withBody("Service Unavailable")));

        assertThatThrownBy(() -> erpGateway.getPromotionDetails())
            .isInstanceOf(ErpGatewayException.class)
            .hasMessageContaining("503");
    }
}

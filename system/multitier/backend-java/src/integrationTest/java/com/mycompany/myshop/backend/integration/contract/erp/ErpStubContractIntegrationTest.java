package com.mycompany.myshop.backend.integration.contract.erp;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.mycompany.myshop.backend.core.services.external.ErpGateway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The {@code Stub} side of the ERP product contract: raw, inlined WireMock, same shape as
 * {@code legacy/ErpGatewayIntegrationTest}. No Docker beyond what {@code integrationTest} already
 * needs for other classes in this task.
 */
class ErpStubContractIntegrationTest extends BaseErpProductContractIntegrationTest {

    private static final WireMockServer WIRE_MOCK = new WireMockServer(options().dynamicPort());

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

        erpGateway = new ErpGateway();
        ReflectionTestUtils.setField(erpGateway, "erpUrl", WIRE_MOCK.baseUrl());
    }

    @Override
    protected void arrangeProduct(String sku, String price) {
        WIRE_MOCK.stubFor(get("/api/products/" + sku)
            .willReturn(okJson("{\"id\":\"" + sku + "\",\"price\":" + price + "}")));
    }

    @Override
    protected ErpGateway erpGateway() {
        return erpGateway;
    }
}

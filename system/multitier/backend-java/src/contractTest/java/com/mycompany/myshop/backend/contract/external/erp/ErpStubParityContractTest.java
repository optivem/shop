package com.mycompany.myshop.backend.contract.external.erp;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.mycompany.myshop.backend.core.services.external.ErpGateway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * The {@code Stub} side of the ERP product contract: raw, inlined WireMock, same shape as
 * {@code integration/legacy/ErpGatewayIntegrationTest}. Needs no Docker of its own — the gateway is
 * driven directly, with no Spring context and no container, so the only reason this class sits in
 * the Docker-requiring {@code external-contract} suite is that the stub-consumability tests it runs
 * alongside boot the full SUT.
 */
class ErpStubParityContractTest extends BaseErpProductParityContractTest {

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

        erpGateway = new ErpGateway(WIRE_MOCK.baseUrl());
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

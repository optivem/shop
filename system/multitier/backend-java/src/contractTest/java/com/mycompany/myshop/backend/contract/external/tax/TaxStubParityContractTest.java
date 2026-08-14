package com.mycompany.myshop.backend.contract.external.tax;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.mycompany.myshop.backend.core.services.external.TaxGateway;
import com.mycompany.myshop.backend.testkit.driver.adapter.external.tax.TaxStubDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The {@code Stub} side of the tax country contract. Unlike {@code ErpStubParityContractTest}, which
 * inlines its WireMock mapping, this arranges through the real {@link TaxStubDriver} — so what gets
 * held against the simulator by {@link TaxRealParityContractTest} is the driver's own JSON, which is
 * the artifact the parity pair exists to guard.
 *
 * <p>Needs no Docker of its own: the gateway is driven directly, with no Spring context and no
 * container.
 */
class TaxStubParityContractTest extends BaseTaxCountryParityContractTest {

    private static final WireMockServer WIRE_MOCK = new WireMockServer(options().dynamicPort());

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

        taxGateway = new TaxGateway();
        ReflectionTestUtils.setField(taxGateway, "taxUrl", WIRE_MOCK.baseUrl());
    }

    @Override
    protected void arrangeCountry(String code, String taxRate) {
        new TaxStubDriver(new WireMock(WIRE_MOCK.port())).returnsTaxRate(code, taxRate);
    }

    @Override
    protected TaxGateway taxGateway() {
        return taxGateway;
    }
}

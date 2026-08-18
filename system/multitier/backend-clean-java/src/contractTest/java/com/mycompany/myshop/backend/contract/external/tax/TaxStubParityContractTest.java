package com.mycompany.myshop.backend.contract.external.tax;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.mycompany.myshop.backend.domain.gateways.TaxGateway;
import com.mycompany.myshop.backend.infrastructure.external.tax.HttpTaxGateway;
import com.mycompany.myshop.backend.testkit.driver.adapter.external.tax.TaxStubDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

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

        taxGateway = new HttpTaxGateway(WIRE_MOCK.baseUrl());
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

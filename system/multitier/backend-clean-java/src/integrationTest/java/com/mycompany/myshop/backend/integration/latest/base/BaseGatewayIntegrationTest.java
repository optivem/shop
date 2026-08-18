package com.mycompany.myshop.backend.integration.latest.base;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.mycompany.myshop.backend.backendtest.configuration.ExternalSystemMode;
import com.mycompany.myshop.backend.backendtest.configuration.StubDrivers;
import com.mycompany.myshop.backend.domain.gateways.ClockGateway;
import com.mycompany.myshop.backend.domain.gateways.ErpGateway;
import com.mycompany.myshop.backend.domain.gateways.TaxGateway;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.clock.ClockDsl;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.erp.ErpDsl;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.tax.TaxDsl;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseGatewayIntegrationTest {

    protected static final WireMockServer WIRE_MOCK = new WireMockServer(options().dynamicPort());

    static {
        WIRE_MOCK.start();
    }

    private ExternalSystemMode externalSystemMode;

    private ErpDsl erp;
    private TaxDsl tax;
    private ClockDsl clock;

    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.STUB;
    }

    @BeforeEach
    void resetGatewayHarness() {
        WIRE_MOCK.resetAll();
        externalSystemMode = getFixedExternalSystemMode();

        // Cleared rather than rebuilt: the accessors below re-create on demand, so a test that touches
        // only one external never pays for the other two.
        erp = null;
        tax = null;
        clock = null;
    }

    protected final String stubBaseUrl() {
        return WIRE_MOCK.baseUrl();
    }

    protected final ErpDsl erp() {
        if (erp == null) {
            erp = new ErpDsl(StubDrivers.erp(WIRE_MOCK));
        }
        return erp;
    }

    protected final TaxDsl tax() {
        if (tax == null) {
            tax = new TaxDsl(StubDrivers.tax(WIRE_MOCK));
        }
        return tax;
    }

    protected final ClockDsl clock() {
        if (clock == null) {
            clock = new ClockDsl(StubDrivers.clock(WIRE_MOCK));
        }
        return clock;
    }

    protected final ErpGateway erpGateway() {
        return Gateways.erp(stubBaseUrl());
    }

    protected final TaxGateway taxGateway() {
        return Gateways.tax(stubBaseUrl());
    }

    protected final ClockGateway clockGateway() {
        return clockGateway(externalSystemMode);
    }

    protected final ClockGateway clockGateway(ExternalSystemMode mode) {
        return Gateways.clock(stubBaseUrl(), mode);
    }

    protected final ClockGateway clockGatewayWithRawMode(String rawMode) {
        return Gateways.clockWithRawMode(stubBaseUrl(), rawMode);
    }
}

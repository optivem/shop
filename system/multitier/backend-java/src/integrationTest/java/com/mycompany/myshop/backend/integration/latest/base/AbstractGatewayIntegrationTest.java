package com.mycompany.myshop.backend.integration.latest.base;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.mycompany.myshop.backend.core.services.external.ClockGateway;
import com.mycompany.myshop.backend.core.services.external.ErpGateway;
import com.mycompany.myshop.backend.core.services.external.TaxGateway;
import com.mycompany.myshop.backend.support.core.usecase.external.clock.ClockDsl;
import com.mycompany.myshop.backend.support.core.usecase.external.erp.ErpDsl;
import com.mycompany.myshop.backend.support.core.usecase.external.tax.TaxDsl;
import com.mycompany.myshop.backend.support.configuration.ExternalSystemMode;
import com.mycompany.myshop.backend.support.configuration.StubDrivers;
import org.junit.jupiter.api.BeforeEach;

/**
 * Narrow-integration harness: one gateway driven directly against an in-process {@link WireMockServer},
 * with its stubs declared through the same use case DSL the component tests reach as {@code app.erp()}
 * / {@code app.tax()} / {@code app.clock()}. No Spring context, no Docker.
 *
 * <p>Every accessor is lazy, which is what lets all three externals live on one base class: a
 * narrow-integration test drives exactly one gateway, so the other two are never built. This mirrors
 * the system-test {@code BaseConfigurableTest}, minus the parts that have no meaning here — there is
 * no channel to choose, and nothing to close (the stub drivers hold a WireMock client, not a resource).
 *
 * <p>WireMock follows the component harness's singleton-container pattern — started once in a static
 * initializer and never stopped, reset per test — rather than {@code @BeforeAll}/{@code @AfterAll}.
 * Both would be defensible for a single test class, but a base class's {@code @BeforeAll} runs once
 * per *subclass*, so lifting the old per-class start/stop up here would start an already-running
 * server on the second subclass. Starting once also leaves the module with a single WireMock lifecycle
 * rule across the component and narrow-integration layers instead of two competing ones.
 */
public abstract class AbstractGatewayIntegrationTest {

    protected static final WireMockServer WIRE_MOCK = new WireMockServer(options().dynamicPort());

    static {
        WIRE_MOCK.start();
    }

    private ExternalSystemMode externalSystemMode;

    private ErpDsl erp;
    private TaxDsl tax;
    private ClockDsl clock;

    /**
     * The mode the SUT runs in for this test class. Mirrors {@code BaseConfigurableTest}'s
     * {@code getFixedExternalSystemMode()}; a class that needs a different mode overrides it, and a
     * single test that needs to deviate builds its gateway explicitly via
     * {@link #clockGateway(ExternalSystemMode)}.
     */
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

    /** The clock gateway in this class's fixed mode. */
    protected final ClockGateway clockGateway() {
        return clockGateway(externalSystemMode);
    }

    /** The clock gateway in an explicit mode, for a test that deviates from the class default. */
    protected final ClockGateway clockGateway(ExternalSystemMode mode) {
        return Gateways.clock(stubBaseUrl(), mode);
    }

    /** See {@link Gateways#clockWithRawMode} — for pinning the SUT's unknown-mode branch. */
    protected final ClockGateway clockGatewayWithRawMode(String rawMode) {
        return Gateways.clockWithRawMode(stubBaseUrl(), rawMode);
    }
}

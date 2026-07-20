package com.mycompany.myshop.backend.support.core.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myshop.backend.support.harness.BackendDriver;
import com.mycompany.myshop.backend.support.harness.ClockStubDriver;
import com.mycompany.myshop.backend.support.harness.ErpStubDriver;
import com.mycompany.myshop.backend.support.harness.SutClockReader;
import com.mycompany.myshop.backend.support.harness.SutErpReader;
import com.mycompany.myshop.backend.support.harness.SutTaxReader;
import com.mycompany.myshop.backend.support.harness.TaxStubDriver;
import com.mycompany.myshop.backend.support.core.usecase.external.clock.ClockDsl;
import com.mycompany.myshop.backend.support.core.usecase.external.erp.ErpDsl;
import com.mycompany.myshop.backend.support.core.usecase.external.tax.TaxDsl;

/**
 * Root of the use case layer: one entry per actor in a component test — the system under test
 * ({@link #myShop()}) and the three external systems it talks to ({@link #erp()}, {@link #tax()},
 * {@link #clock()}).
 *
 * <p>This is the layer the scenario DSL is built on, and it stays exposed: a test that needs
 * surgical control can drop to it directly instead of bending a scenario around an edge case.
 */
public class UseCaseDsl {

    private final MyShopDsl myShop;
    private final ErpDsl erp;
    private final TaxDsl tax;
    private final ClockDsl clock;
    private final SutErpReader sutErp;
    private final SutTaxReader sutTax;
    private final SutClockReader sutClock;

    public UseCaseDsl(
            BackendDriver backendDriver,
            ObjectMapper objectMapper,
            ErpStubDriver erpStubDriver,
            TaxStubDriver taxStubDriver,
            ClockStubDriver clockStubDriver,
            SutErpReader sutErp,
            SutTaxReader sutTax,
            SutClockReader sutClock) {
        this.myShop = new MyShopDsl(backendDriver, objectMapper);
        this.erp = new ErpDsl(erpStubDriver);
        this.tax = new TaxDsl(taxStubDriver);
        this.clock = new ClockDsl(clockStubDriver);
        this.sutErp = sutErp;
        this.sutTax = sutTax;
        this.sutClock = sutClock;
    }

    public MyShopDsl myShop() {
        return myShop;
    }

    public ErpDsl erp() {
        return erp;
    }

    public TaxDsl tax() {
        return tax;
    }

    public ClockDsl clock() {
        return clock;
    }

    /**
     * Reads a product through the SUT's production {@code ErpGateway} — the SUT's own view of the ERP
     * stub. Backs {@code then().product(...)} in the stub-contract tests, deliberately not the stub
     * client (a test-side read of the stub would be a tautology).
     */
    public SutErpReader sutErp() {
        return sutErp;
    }

    /** Reads a country through the SUT's production {@code TaxGateway}. See {@link #sutErp()}. */
    public SutTaxReader sutTax() {
        return sutTax;
    }

    /** Reads the current time through the SUT's production {@code ClockGateway}. See {@link #sutErp()}. */
    public SutClockReader sutClock() {
        return sutClock;
    }
}

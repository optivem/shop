package com.mycompany.myshop.backend.testkit.dsl.core.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myshop.backend.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.backend.testkit.driver.port.external.clock.ClockDriver;
import com.mycompany.myshop.backend.testkit.driver.port.external.erp.ErpDriver;
import com.mycompany.myshop.backend.testkit.driver.adapter.sut.SutClockReader;
import com.mycompany.myshop.backend.testkit.driver.adapter.sut.SutErpReader;
import com.mycompany.myshop.backend.testkit.driver.adapter.sut.SutTaxReader;
import com.mycompany.myshop.backend.testkit.driver.port.external.tax.TaxDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.clock.ClockDsl;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.erp.ErpDsl;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.tax.TaxDsl;

public class UseCaseDsl {

    private final MyShopDsl myShop;
    private final ErpDsl erp;
    private final TaxDsl tax;
    private final ClockDsl clock;
    private final SutErpReader sutErp;
    private final SutTaxReader sutTax;
    private final SutClockReader sutClock;

    public UseCaseDsl(
            MyShopDriver backendDriver,
            ObjectMapper objectMapper,
            ErpDriver erpStubDriver,
            TaxDriver taxStubDriver,
            ClockDriver clockStubDriver,
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

    public SutErpReader sutErp() {
        return sutErp;
    }

    public SutTaxReader sutTax() {
        return sutTax;
    }

    public SutClockReader sutClock() {
        return sutClock;
    }
}

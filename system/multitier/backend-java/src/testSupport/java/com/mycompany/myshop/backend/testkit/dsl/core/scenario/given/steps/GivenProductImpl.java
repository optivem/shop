package com.mycompany.myshop.backend.support.core.scenario.given.steps;

import com.mycompany.myshop.backend.support.core.scenario.ScenarioDefaults;
import com.mycompany.myshop.backend.support.core.scenario.given.GivenImpl;
import com.mycompany.myshop.backend.support.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.support.port.given.steps.GivenProduct;
import java.math.BigDecimal;

public class GivenProductImpl extends BaseGivenStep implements GivenProduct {

    private String sku;
    private String unitPrice;
    private boolean exists = true;

    public GivenProductImpl(GivenImpl given) {
        super(given);
        withSku(ScenarioDefaults.DEFAULT_SKU);
        withUnitPrice(ScenarioDefaults.DEFAULT_UNIT_PRICE);
    }

    @Override
    public GivenProductImpl withSku(String sku) {
        this.sku = sku;
        return this;
    }

    @Override
    public GivenProductImpl withUnitPrice(String unitPrice) {
        this.unitPrice = unitPrice;
        return this;
    }

    @Override
    public GivenProductImpl withUnitPrice(double unitPrice) {
        return withUnitPrice(BigDecimal.valueOf(unitPrice).toPlainString());
    }

    @Override
    public GivenProductImpl doesNotExist() {
        this.exists = false;
        return this;
    }

    @Override
    public void execute(UseCaseDsl app) {
        if (exists) {
            app.erp().returnsProduct().sku(sku).unitPrice(unitPrice).execute();
        } else {
            app.erp().returnsNoProduct().sku(sku).execute();
        }
    }
}

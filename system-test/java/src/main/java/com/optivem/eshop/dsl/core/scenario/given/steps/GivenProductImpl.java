package com.optivem.eshop.dsl.core.scenario.given.steps;

import com.optivem.eshop.dsl.common.Converter;
import com.optivem.eshop.dsl.core.usecase.UseCaseDsl;
import com.optivem.eshop.dsl.core.scenario.given.GivenImpl;
import com.optivem.eshop.dsl.port.given.steps.GivenProduct;

import static com.optivem.eshop.dsl.core.scenario.ScenarioDefaults.DEFAULT_SKU;
import static com.optivem.eshop.dsl.core.scenario.ScenarioDefaults.DEFAULT_UNIT_PRICE;

public class GivenProductImpl extends BaseGivenStep implements GivenProduct {
    private String sku;
    private String unitPrice;

    public GivenProductImpl(GivenImpl given) {
        super(given);
        withSku(DEFAULT_SKU);
        withUnitPrice(DEFAULT_UNIT_PRICE);
    }

    public GivenProductImpl withSku(String sku) {
        this.sku = sku;
        return this;
    }

    public GivenProductImpl withUnitPrice(String unitPrice) {
        this.unitPrice = unitPrice;
        return this;
    }

    public GivenProductImpl withUnitPrice(double unitPrice) {
        withUnitPrice(Converter.fromDouble(unitPrice));
        return this;
    }

    @Override
    public void execute(UseCaseDsl app) {
        app.erp().returnsProduct()
                .sku(sku)
                .unitPrice(unitPrice)
                .execute()
                .shouldSucceed();
    }
}



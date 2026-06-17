package com.mycompany.myshop.testkit.dsl.core.scenario.given.steps;

import com.mycompany.myshop.testkit.common.Converter;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.GivenImpl;
import com.mycompany.myshop.testkit.dsl.port.given.steps.GivenProduct;

import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_SKU;
import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_UNIT_PRICE;
import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_WEIGHT;

public class GivenProductImpl extends BaseGivenStep implements GivenProduct {
    private String sku;
    private String unitPrice;
    private String weight;

    public GivenProductImpl(GivenImpl given) {
        super(given);
        withSku(DEFAULT_SKU);
        withUnitPrice(DEFAULT_UNIT_PRICE);
        this.weight = DEFAULT_WEIGHT;
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
    public GivenProductImpl withWeight(double weight) {
        this.weight = Converter.fromDouble(weight);
        return this;
    }

    @Override
    public void execute(UseCaseDsl app) {
        app.erp().returnsProduct()
                .sku(sku)
                .unitPrice(unitPrice)
                .weight(weight)
                .execute()
                .shouldSucceed();
    }
}



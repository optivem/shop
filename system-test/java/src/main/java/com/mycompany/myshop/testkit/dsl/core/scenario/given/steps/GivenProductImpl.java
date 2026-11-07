package com.mycompany.myshop.testkit.dsl.core.scenario.given.steps;

import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.GivenImpl;
import com.mycompany.myshop.testkit.dsl.port.given.steps.GivenProduct;

import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_SKU;
import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_UNIT_PRICE;

public class GivenProductImpl extends BaseGivenStep implements GivenProduct {
    private String sku;
    private String unitPrice;
    private String title;
    private String description;
    private String category;
    private String brand;

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

    @Override
    public GivenProductImpl withTitle(String title) {
        this.title = title;
        return this;
    }

    @Override
    public GivenProductImpl withDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public GivenProductImpl withCategory(String category) {
        this.category = category;
        return this;
    }

    @Override
    public GivenProductImpl withBrand(String brand) {
        this.brand = brand;
        return this;
    }

    @Override
    public void execute(UseCaseDsl app) {
        app.erp().returnsProduct()
                .sku(sku)
                .unitPrice(unitPrice)
                .title(title)
                .description(description)
                .category(category)
                .brand(brand)
                .execute()
                .shouldSucceed();
    }
}



package com.mycompany.myshop.backend.support.core.scenario.given.steps;

import com.mycompany.myshop.backend.support.core.scenario.ScenarioDefaults;
import com.mycompany.myshop.backend.support.core.scenario.given.GivenImpl;
import com.mycompany.myshop.backend.support.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.support.port.given.steps.GivenOrder;

public class GivenOrderImpl extends BaseGivenStep implements GivenOrder {

    private String sku;
    private int quantity;
    private String country;
    private String couponCode;

    public GivenOrderImpl(GivenImpl given) {
        super(given);
        withSku(ScenarioDefaults.DEFAULT_SKU);
        withQuantity(ScenarioDefaults.DEFAULT_QUANTITY);
        withCountry(ScenarioDefaults.DEFAULT_COUNTRY);
        withCouponCode(ScenarioDefaults.EMPTY);
    }

    @Override
    public GivenOrderImpl withSku(String sku) {
        this.sku = sku;
        return this;
    }

    @Override
    public GivenOrderImpl withQuantity(int quantity) {
        this.quantity = quantity;
        return this;
    }

    @Override
    public GivenOrderImpl withCountry(String country) {
        this.country = country;
        return this;
    }

    @Override
    public GivenOrderImpl withCouponCode(String couponCode) {
        this.couponCode = couponCode;
        return this;
    }

    @Override
    public void execute(UseCaseDsl app) {
        app.myShop().placeOrder()
            .sku(sku)
            .quantity(quantity)
            .country(country)
            .couponCode(couponCode)
            .execute()
            .shouldSucceed();
    }
}

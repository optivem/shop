package com.mycompany.myshop.backend.support.core.scenario.when.steps;

import com.mycompany.myshop.backend.core.dtos.PlaceOrderResponse;
import com.mycompany.myshop.backend.support.core.ScenarioDslImpl;
import com.mycompany.myshop.backend.support.core.scenario.ExecutionResult;
import com.mycompany.myshop.backend.support.core.scenario.ExecutionResultBuilder;
import com.mycompany.myshop.backend.support.core.scenario.ScenarioDefaults;
import com.mycompany.myshop.backend.support.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.support.core.usecase.usecases.PlaceOrderVerification;
import com.mycompany.myshop.backend.support.port.when.steps.WhenPlaceOrder;

public class WhenPlaceOrderImpl extends BaseWhenStep<PlaceOrderResponse, PlaceOrderVerification>
        implements WhenPlaceOrder {

    private String sku = ScenarioDefaults.DEFAULT_SKU;
    private int quantity = ScenarioDefaults.DEFAULT_QUANTITY;
    private String country = ScenarioDefaults.DEFAULT_COUNTRY;
    private String couponCode = ScenarioDefaults.EMPTY;
    private String rawQuantity;
    private boolean quantityIsRaw;

    public WhenPlaceOrderImpl(UseCaseDsl app, ScenarioDslImpl scenario) {
        super(app, scenario);
    }

    @Override
    public WhenPlaceOrderImpl withSku(String sku) {
        this.sku = sku;
        return this;
    }

    @Override
    public WhenPlaceOrderImpl withQuantity(int quantity) {
        this.quantity = quantity;
        this.quantityIsRaw = false;
        return this;
    }

    @Override
    public WhenPlaceOrderImpl withQuantity(String quantity) {
        this.rawQuantity = quantity;
        this.quantityIsRaw = true;
        return this;
    }

    @Override
    public WhenPlaceOrderImpl withCountry(String country) {
        this.country = country;
        return this;
    }

    @Override
    public WhenPlaceOrderImpl withCouponCode(String couponCode) {
        this.couponCode = couponCode;
        return this;
    }

    @Override
    public WhenPlaceOrderImpl withCouponCode() {
        return withCouponCode(ScenarioDefaults.DEFAULT_COUPON_CODE);
    }

    @Override
    protected ExecutionResult<PlaceOrderResponse, PlaceOrderVerification> execute(UseCaseDsl app) {
        var placeOrder = app.myShop().placeOrder()
            .sku(sku)
            .country(country)
            .couponCode(couponCode);

        var result = (quantityIsRaw ? placeOrder.rawQuantity(rawQuantity) : placeOrder.quantity(quantity))
            .execute();

        // The SUT generates the order number, so it can only be read back off an accepted response.
        var placed = result.responseOrNull();

        return new ExecutionResultBuilder<>(result)
            .orderNumber(placed == null ? null : placed.getOrderNumber())
            .couponCode(couponCode)
            .build();
    }
}

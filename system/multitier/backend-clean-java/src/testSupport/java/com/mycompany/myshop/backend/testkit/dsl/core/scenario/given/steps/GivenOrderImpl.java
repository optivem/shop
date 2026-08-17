package com.mycompany.myshop.backend.testkit.dsl.core.scenario.given.steps;

import com.mycompany.myshop.backend.domain.entities.OrderStatus;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.ScenarioDefaults;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.given.GivenImpl;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.testkit.dsl.port.given.steps.GivenOrder;

public class GivenOrderImpl extends BaseGivenStep implements GivenOrder {

    private String orderNumberAlias;
    private String sku;
    private int quantity;
    private String country;
    private String couponCode;
    private OrderStatus status;

    // No default alias: an order nobody names is background, and registering every one of them under
    // the same default would make a second given().order() collide. GivenImpl hands the default to
    // the first order only.
    public GivenOrderImpl(GivenImpl given) {
        super(given);
        withSku(ScenarioDefaults.DEFAULT_SKU);
        withQuantity(ScenarioDefaults.DEFAULT_QUANTITY);
        withCountry(ScenarioDefaults.DEFAULT_COUNTRY);
        withCouponCode(ScenarioDefaults.EMPTY);
        withStatus(ScenarioDefaults.DEFAULT_ORDER_STATUS);
    }

    @Override
    public GivenOrderImpl withStatus(OrderStatus status) {
        this.status = status;
        return this;
    }

    @Override
    public GivenOrderImpl withOrderNumber(String orderNumberAlias) {
        this.orderNumberAlias = orderNumberAlias;
        return this;
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

    // The order number the SUT mints is registered under the alias rather than dropped, so a later
    // step — cancelling this order, most of all — can name it without the test ever seeing it.
    @Override
    public void execute(UseCaseDsl app) {
        app.myShop().placeOrder()
            .orderNumber(orderNumberAlias)
            .sku(sku)
            .quantity(quantity)
            .country(country)
            .couponCode(couponCode)
            .execute()
            .shouldSucceed();

        driveToStatus(app);
    }

    // Reached through the SUT's own endpoints rather than by writing the status column, so the order
    // arrives at when() having gone everywhere production would have taken it.
    private void driveToStatus(UseCaseDsl app) {
        switch (status) {
            case PLACED -> { /* placing is all it takes */ }
            case CANCELLED ->
                app.myShop().cancelOrder().orderNumber(orderNumberAlias).execute().shouldSucceed();
            default ->
                throw new IllegalArgumentException(
                    "given().order().withStatus(" + status + ") is not supported: the deliver "
                        + "endpoint has no driver method yet. Only PLACED and CANCELLED are "
                        + "reachable from the component DSL.");
        }
    }
}

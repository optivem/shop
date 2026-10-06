package com.mycompany.myshop.testkit.dsl.core.scenario.given.steps;

import com.mycompany.myshop.testkit.common.Converter;
import com.mycompany.myshop.testkit.driver.port.UserIdentity;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.GivenImpl;
import com.mycompany.myshop.testkit.dsl.port.given.steps.GivenOrder;
import com.mycompany.myshop.testkit.common.domain.OrderStatus;

import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.*;

public class GivenOrderImpl extends BaseGivenStep implements GivenOrder {
    private String orderNumber;
    private String sku;
    private String quantity;
    private String country;
    private String couponCodeAlias;
    private OrderStatus status;
    private final GivenImpl given;
    private String placedByAlias;

    public GivenOrderImpl(GivenImpl given) {
        super(given);
        this.given = given;

        withOrderNumber(DEFAULT_ORDER_NUMBER);
        withSku(DEFAULT_SKU);
        withQuantity(DEFAULT_QUANTITY);
        withCountry(DEFAULT_COUNTRY);
        withCouponCode(EMPTY);
        withStatus(DEFAULT_ORDER_STATUS);
    }

    public GivenOrderImpl withOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
        return this;
    }

    public GivenOrderImpl withSku(String sku) {
        this.sku = sku;
        return this;
    }

    public GivenOrderImpl withQuantity(String quantity) {
        this.quantity = quantity;
        return this;
    }

    public GivenOrderImpl withQuantity(int quantity) {
        return withQuantity(Converter.fromInteger(quantity));
    }

    public GivenOrderImpl withCountry(String country) {
        this.country = country;
        return this;
    }

    public GivenOrderImpl withCouponCode(String couponCodeAlias) {
        this.couponCodeAlias = couponCodeAlias;
        return this;
    }

    public GivenOrderImpl placedByCustomer() {
        this.placedByAlias = null;
        return this;
    }

    public GivenOrderImpl placedByCustomer(String alias) {
        given.registerCustomer(alias);
        this.placedByAlias = alias;
        return this;
    }

    public boolean isPlacedByDefaultCustomer() {
        return placedByAlias == null;
    }

    public GivenOrderImpl withStatus(OrderStatus status) {
        this.status = status;
        return this;
    }

    public GivenOrderImpl withStatus(String status) {
        return withStatus(OrderStatus.valueOf(status));
    }

    @Override
    public void execute(UseCaseDsl app) {
        app.actAs(placedByAlias == null ? given.defaultCustomer() : given.resolveCustomer(placedByAlias));
        try {
            placeAndCancel(app);
        } finally {
            app.actAs(UserIdentity.DEFAULT);
        }

        if (status == OrderStatus.DELIVERED) {
            app.myShop().deliverOrder()
                    .orderNumber(orderNumber)
                    .execute()
                    .shouldSucceed();
        }
    }

    private void placeAndCancel(UseCaseDsl app) {
        app.myShop().placeOrder()
                .orderNumber(orderNumber)
                .sku(sku)
                .quantity(quantity)
                .country(country)
                .couponCode(couponCodeAlias)
                .execute()
                .shouldSucceed();

        if (status == OrderStatus.CANCELLED) {
            app.myShop().cancelOrder()
                    .orderNumber(orderNumber)
                    .execute()
                    .shouldSucceed();
        }
    }
}

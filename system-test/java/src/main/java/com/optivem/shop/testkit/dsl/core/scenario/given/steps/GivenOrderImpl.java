package com.optivem.shop.testkit.dsl.core.scenario.given.steps;

import com.optivem.shop.testkit.common.Converter;
import com.optivem.shop.testkit.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.testkit.dsl.core.scenario.given.GivenImpl;
import com.optivem.shop.testkit.dsl.port.given.steps.GivenOrder;
import com.optivem.shop.testkit.driver.port.shop.dtos.OrderStatus;

import static com.optivem.shop.testkit.dsl.core.scenario.ScenarioDefaults.*;

public class GivenOrderImpl extends BaseGivenStep implements GivenOrder {
    private String orderNumber;
    private String sku;
    private String quantity;
    private String country;
    private String couponCodeAlias;
    private OrderStatus status;

    public GivenOrderImpl(GivenImpl given) {
        super(given);

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

    public GivenOrderImpl withStatus(OrderStatus status) {
        this.status = status;
        return this;
    }

    public GivenOrderImpl withStatus(String status) {
        return withStatus(OrderStatus.valueOf(status));
    }

    @Override
    public void execute(UseCaseDsl app) {
        app.shop().placeOrder()
                .orderNumber(orderNumber)
                .sku(sku)
                .quantity(quantity)
                .country(country)
                .couponCode(couponCodeAlias)
                .execute()
                .shouldSucceed();

        if (status == OrderStatus.CANCELLED) {
            app.shop().cancelOrder()
                    .orderNumber(orderNumber)
                    .execute()
                    .shouldSucceed();
        }

        if (status == OrderStatus.DELIVERED) {
            app.shop().deliverOrder()
                    .orderNumber(orderNumber)
                    .execute()
                    .shouldSucceed();
        }
    }
}

package com.optivem.eshop.dsl.core.scenario.given.steps;

import com.optivem.eshop.dsl.common.Converter;
import com.optivem.eshop.dsl.core.usecase.UseCaseDsl;
import com.optivem.eshop.dsl.core.scenario.given.GivenImpl;
import com.optivem.eshop.dsl.port.given.steps.GivenOrder;
import com.optivem.eshop.dsl.driver.port.shop.dtos.OrderStatus;

import static com.optivem.eshop.dsl.core.scenario.ScenarioDefaults.*;

public class GivenOrderImpl extends BaseGivenStep implements GivenOrder {
    private String orderNumber;
    private String sku;
    private String quantity;

    public GivenOrderImpl(GivenImpl given) {
        super(given);

        withOrderNumber(DEFAULT_ORDER_NUMBER);
        withSku(DEFAULT_SKU);
        withQuantity(DEFAULT_QUANTITY);
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

    public GivenOrderImpl withStatus(OrderStatus status) {
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
                .execute()
                .shouldSucceed();

    }
}



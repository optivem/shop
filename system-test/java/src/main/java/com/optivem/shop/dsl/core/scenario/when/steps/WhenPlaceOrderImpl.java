package com.optivem.shop.dsl.core.scenario.when.steps;

import static com.optivem.shop.dsl.core.scenario.ScenarioDefaults.*;

import com.optivem.shop.dsl.common.Converter;
import com.optivem.shop.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.dsl.core.scenario.ExecutionResult;
import com.optivem.shop.dsl.core.scenario.ExecutionResultBuilder;
import com.optivem.shop.dsl.driver.port.shop.dtos.PlaceOrderResponse;
import com.optivem.shop.dsl.port.ChannelMode;
import com.optivem.shop.dsl.port.when.steps.WhenPlaceOrder;
import com.optivem.shop.dsl.core.usecase.shop.usecases.PlaceOrderVerification;

public class WhenPlaceOrderImpl extends BaseWhenStep<PlaceOrderResponse, PlaceOrderVerification> implements WhenPlaceOrder {
    private String orderNumber;
    private String sku;
    private String quantity;

    public WhenPlaceOrderImpl(UseCaseDsl app) {
        super(app);
        withOrderNumber(DEFAULT_ORDER_NUMBER);
        withSku(DEFAULT_SKU);
        withQuantity(DEFAULT_QUANTITY);
    }

    public WhenPlaceOrderImpl withOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
        return this;
    }

    public WhenPlaceOrderImpl withSku(String sku) {
        this.sku = sku;
        return this;
    }

    public WhenPlaceOrderImpl withQuantity(String quantity) {
        this.quantity = quantity;
        return this;
    }

    public WhenPlaceOrderImpl withQuantity(int quantity) {
        return withQuantity(Converter.fromInteger(quantity));
    }

    @Override
    protected ExecutionResult<PlaceOrderResponse, PlaceOrderVerification> execute(UseCaseDsl app) {
        var result = app.shop(ChannelMode.DYNAMIC).placeOrder()
                .orderNumber(orderNumber)
                .sku(sku)
                .quantity(quantity)
                .execute();

        return new ExecutionResultBuilder<>(result)
                .orderNumber(orderNumber)
                .build();
    }
}

package com.optivem.shop.dsl.core.scenario.when.steps;

import static com.optivem.shop.dsl.core.scenario.ScenarioDefaults.DEFAULT_ORDER_NUMBER;

import com.optivem.shop.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.dsl.core.scenario.ExecutionResult;
import com.optivem.shop.dsl.core.scenario.ExecutionResultBuilder;
import com.optivem.shop.dsl.driver.port.shop.dtos.ViewOrderResponse;
import com.optivem.shop.dsl.port.ChannelMode;
import com.optivem.shop.dsl.port.when.steps.WhenViewOrder;
import com.optivem.shop.dsl.core.usecase.shop.usecases.ViewOrderVerification;

public class WhenViewOrderImpl extends BaseWhenStep<ViewOrderResponse, ViewOrderVerification> implements WhenViewOrder {
    private String orderNumber;

    public WhenViewOrderImpl(UseCaseDsl app) {
        super(app);
        withOrderNumber(DEFAULT_ORDER_NUMBER);
    }

    public WhenViewOrderImpl withOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
        return this;
    }

    @Override
    protected ExecutionResult<ViewOrderResponse, ViewOrderVerification> execute(UseCaseDsl app) {
        var result = app.shop(ChannelMode.DYNAMIC).viewOrder()
                .orderNumber(orderNumber)
                .execute();

        return new ExecutionResultBuilder<>(result)
                .orderNumber(orderNumber)
                .build();
    }
}

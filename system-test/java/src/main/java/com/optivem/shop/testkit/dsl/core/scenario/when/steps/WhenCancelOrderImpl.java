package com.optivem.shop.testkit.dsl.core.scenario.when.steps;

import static com.optivem.shop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_ORDER_NUMBER;

import com.optivem.shop.testkit.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.testkit.dsl.core.scenario.ExecutionResult;
import com.optivem.shop.testkit.dsl.core.scenario.ExecutionResultBuilder;
import com.optivem.shop.testkit.dsl.core.shared.VoidVerification;
import com.optivem.shop.testkit.dsl.port.ChannelMode;
import com.optivem.shop.testkit.dsl.port.when.steps.WhenCancelOrder;

public class WhenCancelOrderImpl extends BaseWhenStep<Void, VoidVerification> implements WhenCancelOrder {
    private String orderNumber;

    public WhenCancelOrderImpl(UseCaseDsl app) {
        super(app);
        withOrderNumber(DEFAULT_ORDER_NUMBER);
    }

    public WhenCancelOrderImpl withOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
        return this;
    }

    @Override
    protected ExecutionResult<Void, VoidVerification> execute(UseCaseDsl app) {
        var result = app.shop(ChannelMode.DYNAMIC).cancelOrder()
                .orderNumber(orderNumber)
                .execute();

        return new ExecutionResultBuilder<Void, VoidVerification>(result)
                .orderNumber(orderNumber)
                .build();
    }
}

package com.mycompany.myshop.testkit.dsl.core.scenario.when.steps;

import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_ORDER_NUMBER;

import com.mycompany.myshop.testkit.dsl.core.ScenarioDslImpl;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResult;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultBuilder;
import com.mycompany.myshop.testkit.dsl.core.shared.VoidVerification;
import com.mycompany.myshop.testkit.dsl.port.ChannelMode;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenDeliverOrder;

public class WhenDeliverOrderImpl extends BaseWhenStep<Void, VoidVerification> implements WhenDeliverOrder {
    private String orderNumber;

    public WhenDeliverOrderImpl(UseCaseDsl app, ScenarioDslImpl scenario) {
        super(app, scenario);
        withOrderNumber(DEFAULT_ORDER_NUMBER);
    }

    public WhenDeliverOrderImpl withOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
        return this;
    }

    @Override
    protected ExecutionResult<Void, VoidVerification> execute(UseCaseDsl app) {
        var result = app.myShop(ChannelMode.DYNAMIC).deliverOrder()
                .orderNumber(orderNumber)
                .execute();

        return new ExecutionResultBuilder<Void, VoidVerification>(result)
                .orderNumber(orderNumber)
                .build();
    }
}

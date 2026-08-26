package com.mycompany.myshop.backend.testkit.dsl.core.scenario.when.steps;

import com.mycompany.myshop.backend.usecases.queries.order.ViewOrderDetailsResponse;
import com.mycompany.myshop.backend.testkit.dsl.core.ScenarioDslImpl;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.ExecutionResult;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.ExecutionResultBuilder;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.ViewOrderVerification;
import com.mycompany.myshop.backend.testkit.dsl.port.when.steps.WhenViewOrder;

public class WhenViewOrderImpl
        extends BaseWhenStep<ViewOrderDetailsResponse, ViewOrderVerification>
        implements WhenViewOrder {

    private String orderNumber;

    public WhenViewOrderImpl(UseCaseDsl app, ScenarioDslImpl scenario) {
        super(app, scenario);
    }

    @Override
    public WhenViewOrderImpl withOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
        return this;
    }

    @Override
    protected ExecutionResult<ViewOrderDetailsResponse, ViewOrderVerification> execute(
            UseCaseDsl app) {
        var result = app.myShop().viewOrder().orderNumber(orderNumber).execute();

        return new ExecutionResultBuilder<>(result)
            .orderNumber(orderNumber)
            .build();
    }
}

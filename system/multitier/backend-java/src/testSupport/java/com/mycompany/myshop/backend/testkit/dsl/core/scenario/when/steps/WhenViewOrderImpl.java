package com.mycompany.myshop.backend.support.core.scenario.when.steps;

import com.mycompany.myshop.backend.core.dtos.ViewOrderDetailsResponse;
import com.mycompany.myshop.backend.support.core.ScenarioDslImpl;
import com.mycompany.myshop.backend.support.core.scenario.ExecutionResult;
import com.mycompany.myshop.backend.support.core.scenario.ExecutionResultBuilder;
import com.mycompany.myshop.backend.support.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.support.core.usecase.usecases.ViewOrderVerification;
import com.mycompany.myshop.backend.support.port.when.steps.WhenViewOrder;

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

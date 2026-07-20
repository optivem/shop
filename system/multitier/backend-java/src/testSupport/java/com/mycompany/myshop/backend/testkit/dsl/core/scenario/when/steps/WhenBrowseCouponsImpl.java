package com.mycompany.myshop.backend.support.core.scenario.when.steps;

import com.mycompany.myshop.backend.core.dtos.BrowseCouponsResponse;
import com.mycompany.myshop.backend.support.core.ScenarioDslImpl;
import com.mycompany.myshop.backend.support.core.scenario.ExecutionResult;
import com.mycompany.myshop.backend.support.core.scenario.ExecutionResultBuilder;
import com.mycompany.myshop.backend.support.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.support.core.usecase.usecases.BrowseCouponsVerification;
import com.mycompany.myshop.backend.support.port.when.steps.WhenBrowseCoupons;

public class WhenBrowseCouponsImpl
        extends BaseWhenStep<BrowseCouponsResponse, BrowseCouponsVerification>
        implements WhenBrowseCoupons {

    public WhenBrowseCouponsImpl(UseCaseDsl app, ScenarioDslImpl scenario) {
        super(app, scenario);
    }

    @Override
    protected ExecutionResult<BrowseCouponsResponse, BrowseCouponsVerification> execute(
            UseCaseDsl app) {
        var result = app.myShop().browseCoupons().execute();
        return new ExecutionResultBuilder<>(result).build();
    }
}

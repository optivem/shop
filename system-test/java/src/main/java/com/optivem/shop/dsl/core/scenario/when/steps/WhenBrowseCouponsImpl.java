package com.optivem.shop.dsl.core.scenario.when.steps;

import com.optivem.shop.dsl.core.scenario.ExecutionResult;
import com.optivem.shop.dsl.core.scenario.ExecutionResultBuilder;
import com.optivem.shop.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.dsl.core.usecase.shop.usecases.BrowseCouponsVerification;
import com.optivem.shop.dsl.driver.port.shop.dtos.BrowseCouponsResponse;
import com.optivem.shop.dsl.port.when.steps.WhenBrowseCoupons;

public class WhenBrowseCouponsImpl extends BaseWhenStep<BrowseCouponsResponse, BrowseCouponsVerification> implements WhenBrowseCoupons {

    public WhenBrowseCouponsImpl(UseCaseDsl app) {
        super(app);
    }

    @Override
    protected ExecutionResult<BrowseCouponsResponse, BrowseCouponsVerification> execute(UseCaseDsl app) {
        var result = app.shop().browseCoupons().execute();
        return new ExecutionResultBuilder<>(result).build();
    }
}

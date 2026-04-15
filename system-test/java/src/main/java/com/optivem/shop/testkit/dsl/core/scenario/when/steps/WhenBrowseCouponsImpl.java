package com.optivem.shop.testkit.dsl.core.scenario.when.steps;

import com.optivem.shop.testkit.dsl.core.scenario.ExecutionResult;
import com.optivem.shop.testkit.dsl.core.scenario.ExecutionResultBuilder;
import com.optivem.shop.testkit.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.testkit.dsl.core.usecase.shop.usecases.BrowseCouponsVerification;
import com.optivem.shop.testkit.driver.port.shop.dtos.BrowseCouponsResponse;
import com.optivem.shop.testkit.dsl.port.ChannelMode;
import com.optivem.shop.testkit.dsl.port.when.steps.WhenBrowseCoupons;

public class WhenBrowseCouponsImpl extends BaseWhenStep<BrowseCouponsResponse, BrowseCouponsVerification> implements WhenBrowseCoupons {

    public WhenBrowseCouponsImpl(UseCaseDsl app) {
        super(app);
    }

    @Override
    protected ExecutionResult<BrowseCouponsResponse, BrowseCouponsVerification> execute(UseCaseDsl app) {
        var result = app.shop(ChannelMode.DYNAMIC).browseCoupons().execute();
        return new ExecutionResultBuilder<>(result).build();
    }
}

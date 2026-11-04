package com.mycompany.myshop.testkit.dsl.core.scenario.when.steps;

import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResult;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultBuilder;
import com.mycompany.myshop.testkit.dsl.core.ScenarioDslImpl;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.BrowseCouponsVerification;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCouponsResponse;
import com.mycompany.myshop.testkit.dsl.port.ChannelMode;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenBrowseCoupons;

public class WhenBrowseCouponsImpl extends BaseWhenStep<BrowseCouponsResponse, BrowseCouponsVerification> implements WhenBrowseCoupons {
    private String page;
    private String size;

    public WhenBrowseCouponsImpl(UseCaseDsl app, ScenarioDslImpl scenario) {
        super(app, scenario);
    }

    public WhenBrowseCouponsImpl withPage(String page) {
        this.page = page;
        return this;
    }

    public WhenBrowseCouponsImpl withSize(String size) {
        this.size = size;
        return this;
    }

    @Override
    protected ExecutionResult<BrowseCouponsResponse, BrowseCouponsVerification> execute(UseCaseDsl app) {
        var result = app.myShop(ChannelMode.DYNAMIC).browseCoupons()
                .page(page)
                .size(size)
                .execute();
        return new ExecutionResultBuilder<>(result).build();
    }
}

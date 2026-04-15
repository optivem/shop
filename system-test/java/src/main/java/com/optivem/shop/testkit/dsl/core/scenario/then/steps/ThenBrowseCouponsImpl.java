package com.optivem.shop.testkit.dsl.core.scenario.then.steps;

import com.optivem.shop.testkit.dsl.core.scenario.ExecutionResultContext;
import com.optivem.shop.testkit.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.testkit.dsl.core.usecase.shop.usecases.BrowseCouponsVerification;
import com.optivem.shop.testkit.dsl.core.shared.VoidVerification;
import com.optivem.shop.testkit.dsl.port.then.steps.ThenBrowseCoupons;

public class ThenBrowseCouponsImpl extends BaseThenStep<Void, VoidVerification> implements ThenBrowseCoupons {
    private final BrowseCouponsVerification verification;

    public ThenBrowseCouponsImpl(UseCaseDsl app, ExecutionResultContext executionResult, BrowseCouponsVerification verification) {
        super(app, executionResult, null);
        this.verification = verification;
    }

    @Override
    public ThenBrowseCouponsImpl containsCouponWithCode(String expectedCode) {
        verification.containsCouponWithCode(expectedCode);
        return this;
    }

    @Override
    public ThenBrowseCouponsImpl couponCount(int expectedCount) {
        verification.couponCount(expectedCount);
        return this;
    }

    @Override
    public ThenBrowseCouponsImpl and() {
        return this;
    }
}

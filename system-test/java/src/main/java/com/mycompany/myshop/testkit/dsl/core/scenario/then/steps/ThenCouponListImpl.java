package com.mycompany.myshop.testkit.dsl.core.scenario.then.steps;

import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.BrowseCouponsVerification;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenCouponList;

import java.util.Arrays;

public class ThenCouponListImpl<R, V extends ResponseVerification<R>>
        extends BaseThenStep<R, V> implements ThenCouponList {
    private final BrowseCouponsVerification verification;

    public ThenCouponListImpl(UseCaseDsl app, ExecutionResultContext executionResult, V successVerification) {
        super(app, executionResult, successVerification);
        if (!(successVerification instanceof BrowseCouponsVerification browseVerification)) {
            throw new IllegalStateException("Cannot verify the coupon list: the executed operation did not browse the coupons");
        }
        this.verification = browseVerification;
    }

    @Override
    public ThenCouponListImpl<R, V> startsWithCoupons(String... couponCodes) {
        verification.startsWithCoupons(Arrays.asList(couponCodes));
        return this;
    }

    @Override
    public ThenCouponListImpl<R, V> and() {
        return this;
    }
}

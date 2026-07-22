package com.mycompany.myshop.testkit.dsl.core.scenario.then.steps;

import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenCoupon;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.BrowseCouponsVerification;

public class ThenCouponImpl<R, V extends ResponseVerification<R>>
        extends BaseThenStep<R, V> implements ThenCoupon {
    private final BrowseCouponsVerification verification;
    private final String couponCode;

    public ThenCouponImpl(UseCaseDsl app, ExecutionResultContext executionResult, String couponCode, V successVerification) {
        super(app, executionResult, successVerification);
        this.couponCode = couponCode;
        this.verification = app.myShop().browseCoupons()
                .execute()
                .shouldSucceed();

        verification.hasCouponWithCode(couponCode);
    }

    public ThenCouponImpl<R, V> hasDiscountRate(double discountRate) {
        verification.couponHasDiscountRate(couponCode, discountRate);
        return this;
    }

    public ThenCouponImpl<R, V> hasDiscountRate(String discountRate) {
        verification.couponHasDiscountRate(couponCode, discountRate);
        return this;
    }

    public ThenCouponImpl<R, V> isValidFrom(String validFrom) {
        verification.couponHasValidFrom(couponCode, validFrom);
        return this;
    }

    public ThenCouponImpl<R, V> isValidTo(String validTo) {
        verification.couponHasValidTo(couponCode, validTo);
        return this;
    }

    public ThenCouponImpl<R, V> hasUsageLimit(int usageLimit) {
        verification.couponHasUsageLimit(couponCode, usageLimit);
        return this;
    }

    public ThenCouponImpl<R, V> hasUsedCount(int expectedUsedCount) {
        verification.couponHasUsedCount(couponCode, expectedUsedCount);
        return this;
    }

    @Override
    public ThenCouponImpl<R, V> and() {
        return this;
    }
}

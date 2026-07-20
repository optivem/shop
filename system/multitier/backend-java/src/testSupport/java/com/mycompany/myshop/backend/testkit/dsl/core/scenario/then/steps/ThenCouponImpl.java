package com.mycompany.myshop.backend.support.core.scenario.then.steps;

import com.mycompany.myshop.backend.support.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.backend.support.core.shared.ResponseVerification;
import com.mycompany.myshop.backend.support.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.support.core.usecase.usecases.BrowseCouponsVerification;
import com.mycompany.myshop.backend.support.port.then.steps.ThenCoupon;

/** Reads the coupons back through {@code GET /api/coupons}; constructing the step asserts the coupon is there. */
public class ThenCouponImpl<R, V extends ResponseVerification<R>> extends BaseThenStep<R, V>
        implements ThenCoupon {

    private final BrowseCouponsVerification verification;
    private final String couponCode;

    public ThenCouponImpl(
            UseCaseDsl app,
            ExecutionResultContext executionResult,
            String couponCode,
            V successVerification) {
        super(app, executionResult, successVerification);
        this.couponCode = couponCode;
        this.verification = app.myShop().browseCoupons().execute().shouldSucceed();
        verification.hasCouponWithCode(couponCode);
    }

    @Override
    public ThenCouponImpl<R, V> hasDiscountRate(String expectedDiscountRate) {
        verification.couponHasDiscountRate(couponCode, expectedDiscountRate);
        return this;
    }

    @Override
    public ThenCouponImpl<R, V> hasUsageLimit(int expectedUsageLimit) {
        verification.couponHasUsageLimit(couponCode, expectedUsageLimit);
        return this;
    }

    @Override
    public ThenCouponImpl<R, V> hasUsedCount(int expectedUsedCount) {
        verification.couponHasUsedCount(couponCode, expectedUsedCount);
        return this;
    }

    @Override
    public ThenCouponImpl<R, V> and() {
        return this;
    }
}

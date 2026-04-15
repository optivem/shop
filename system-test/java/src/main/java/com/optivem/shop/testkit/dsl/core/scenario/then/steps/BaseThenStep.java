package com.optivem.shop.testkit.dsl.core.scenario.then.steps;

import com.optivem.shop.testkit.dsl.core.shared.ResponseVerification;
import com.optivem.shop.testkit.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.testkit.dsl.core.scenario.ExecutionResultContext;
import com.optivem.shop.testkit.dsl.core.usecase.shop.usecases.BrowseCouponsVerification;

public abstract class BaseThenStep<TSuccessResponse, TSuccessVerification extends ResponseVerification<TSuccessResponse>> {
    protected final UseCaseDsl app;
    protected final ExecutionResultContext executionResult;
    protected final TSuccessVerification successVerification;

    protected BaseThenStep(UseCaseDsl app, ExecutionResultContext executionResult, TSuccessVerification successVerification) {
        this.app = app;
        this.executionResult = executionResult;
        this.successVerification = successVerification;
    }

    public BaseThenStep<TSuccessResponse, TSuccessVerification> and() {
        return this;
    }

    public ThenOrderImpl<TSuccessResponse, TSuccessVerification> order(String orderNumber) {
        return new ThenOrderImpl<>(app, executionResult, orderNumber, successVerification);
    }

    public ThenOrderImpl<TSuccessResponse, TSuccessVerification> order() {
        if (executionResult.getOrderNumber() == null) {
            throw new IllegalStateException("Cannot verify order: no order number available from the executed operation");
        }
        return order(executionResult.getOrderNumber());
    }

    public ThenClockImpl clock() {
        var verification = app.clock().getTime().execute().shouldSucceed();
        return new ThenClockImpl(app, executionResult, verification);
    }

    public ThenProductImpl product(String skuAlias) {
        var verification = app.erp().getProduct().sku(skuAlias).execute().shouldSucceed();
        return new ThenProductImpl(app, executionResult, verification);
    }

    public ThenCountryImpl country(String countryAlias) {
        var verification = app.tax().getTaxRate().country(countryAlias).execute().shouldSucceed();
        return new ThenCountryImpl(app, executionResult, verification);
    }

    public ThenBrowseCouponsImpl coupons() {
        BrowseCouponsVerification verification;
        if (successVerification instanceof BrowseCouponsVerification browseCouponsVerification) {
            verification = browseCouponsVerification;
        } else {
            verification = app.shop().browseCoupons().execute().shouldSucceed();
        }
        return new ThenBrowseCouponsImpl(app, executionResult, verification);
    }

    public ThenCouponImpl<TSuccessResponse, TSuccessVerification> coupon(String couponCode) {
        return new ThenCouponImpl<>(app, executionResult, couponCode, successVerification);
    }

    public ThenCouponImpl<TSuccessResponse, TSuccessVerification> coupon() {
        return coupon(executionResult.getCouponCode());
    }

}

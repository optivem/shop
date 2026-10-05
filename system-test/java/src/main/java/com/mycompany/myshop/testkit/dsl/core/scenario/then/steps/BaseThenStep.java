package com.mycompany.myshop.testkit.dsl.core.scenario.then.steps;

import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultContext;

public abstract class BaseThenStep<R, V extends ResponseVerification<R>> {
    protected final UseCaseDsl app;
    protected final ExecutionResultContext executionResult;
    protected final V successVerification;

    protected BaseThenStep(UseCaseDsl app, ExecutionResultContext executionResult, V successVerification) {
        this.app = app;
        this.executionResult = executionResult;
        this.successVerification = successVerification;
    }

    public BaseThenStep<R, V> and() {
        return this;
    }

    public ThenOrderImpl<R, V> order(String orderNumber) {
        return new ThenOrderImpl<>(app, executionResult, orderNumber, successVerification);
    }

    public ThenOrderImpl<R, V> order() {
        if (executionResult.getOrderNumber() == null) {
            throw new IllegalStateException("Cannot verify order: no order number available from the executed operation");
        }
        return order(executionResult.getOrderNumber());
    }

    public ThenOrderHistoryImpl<R, V> orderHistory() {
        return new ThenOrderHistoryImpl<>(app, executionResult, successVerification);
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

    public ThenCouponImpl<R, V> coupon(String couponCode) {
        return new ThenCouponImpl<>(app, executionResult, couponCode, successVerification);
    }

    public ThenCouponImpl<R, V> coupon() {
        return coupon(executionResult.getCouponCode());
    }

}

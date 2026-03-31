package com.optivem.eshop.dsl.core.scenario.then.steps;

import com.optivem.eshop.dsl.core.shared.ResponseVerification;
import com.optivem.eshop.dsl.core.usecase.UseCaseDsl;
import com.optivem.eshop.dsl.core.scenario.ExecutionResultContext;

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

}




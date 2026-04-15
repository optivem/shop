package com.optivem.shop.testkit.dsl.core.scenario.when.steps;

import com.optivem.shop.testkit.dsl.core.shared.ResponseVerification;
import com.optivem.shop.testkit.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.testkit.dsl.core.scenario.ExecutionResult;
import com.optivem.shop.testkit.dsl.core.scenario.then.ThenResultImpl;

public abstract class BaseWhenStep<TSuccessResponse, TSuccessVerification extends ResponseVerification<TSuccessResponse>> {
    private final UseCaseDsl app;

    protected BaseWhenStep(UseCaseDsl app) {
        this.app = app;
    }
    public ThenResultImpl<TSuccessResponse, TSuccessVerification> then() {
        var result = execute(app);
        return new ThenResultImpl<>(app, result);
    }

    protected abstract ExecutionResult<TSuccessResponse, TSuccessVerification> execute(UseCaseDsl app);
}




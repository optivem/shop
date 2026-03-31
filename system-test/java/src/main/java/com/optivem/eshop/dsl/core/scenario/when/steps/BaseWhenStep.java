package com.optivem.eshop.dsl.core.scenario.when.steps;

import com.optivem.eshop.dsl.core.shared.ResponseVerification;
import com.optivem.eshop.dsl.core.usecase.UseCaseDsl;
import com.optivem.eshop.dsl.core.scenario.ExecutionResult;
import com.optivem.eshop.dsl.core.scenario.then.ThenResultImpl;

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




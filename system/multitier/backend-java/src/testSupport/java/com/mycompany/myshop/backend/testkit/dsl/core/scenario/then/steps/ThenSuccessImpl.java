package com.mycompany.myshop.backend.support.core.scenario.then.steps;

import com.mycompany.myshop.backend.support.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.backend.support.core.shared.ResponseVerification;
import com.mycompany.myshop.backend.support.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.support.port.then.steps.ThenSuccess;

public class ThenSuccessImpl<R, V extends ResponseVerification<R>> extends BaseThenStep<R, V>
        implements ThenSuccess {

    public ThenSuccessImpl(
            UseCaseDsl app, ExecutionResultContext executionResult, V successVerification) {
        super(app, executionResult, successVerification);
    }

    @Override
    public ThenSuccessImpl<R, V> and() {
        return this;
    }
}

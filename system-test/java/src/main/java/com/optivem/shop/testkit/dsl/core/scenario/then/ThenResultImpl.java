package com.optivem.shop.testkit.dsl.core.scenario.then;

import com.optivem.shop.testkit.dsl.core.shared.ResponseVerification;
import com.optivem.shop.testkit.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.testkit.dsl.core.scenario.ExecutionResult;
import com.optivem.shop.testkit.dsl.core.scenario.then.steps.ThenFailureImpl;
import com.optivem.shop.testkit.dsl.core.scenario.then.steps.ThenSuccessImpl;
import com.optivem.shop.testkit.dsl.port.then.ThenResultStage;

public class ThenResultImpl<TSuccessResponse, TSuccessVerification extends ResponseVerification<TSuccessResponse>> extends ThenImpl implements ThenResultStage {
    private final ExecutionResult<TSuccessResponse, TSuccessVerification> executionResult;

    public ThenResultImpl(UseCaseDsl app, ExecutionResult<TSuccessResponse, TSuccessVerification> executionResult) {
        super(app);
        this.executionResult = executionResult;
    }

    @Override
    public ThenSuccessImpl<TSuccessResponse, TSuccessVerification> shouldSucceed() {
        var successVerification = executionResult.getResult().shouldSucceed();
        return new ThenSuccessImpl<>(app, executionResult.getContext(), successVerification);
    }

    @Override
    public ThenFailureImpl<TSuccessResponse, TSuccessVerification> shouldFail() {
        return new ThenFailureImpl<>(app, executionResult.getContext(), executionResult.getResult());
    }
}



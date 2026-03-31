package com.optivem.eshop.dsl.core.scenario.then.steps;

import com.optivem.eshop.dsl.core.shared.UseCaseResult;
import com.optivem.eshop.dsl.core.shared.ErrorVerification;
import com.optivem.eshop.dsl.core.shared.ResponseVerification;
import com.optivem.eshop.dsl.core.shared.VoidVerification;
import com.optivem.eshop.dsl.core.usecase.UseCaseDsl;
import com.optivem.eshop.dsl.core.scenario.ExecutionResultContext;
import com.optivem.eshop.dsl.port.then.steps.ThenFailure;

public class ThenFailureImpl<TSuccessResponse, TSuccessVerification extends ResponseVerification<TSuccessResponse>>
        extends BaseThenStep<Void, VoidVerification> implements ThenFailure {
    private final ErrorVerification failureVerification;

    public ThenFailureImpl(UseCaseDsl app, ExecutionResultContext executionResult,
            UseCaseResult<TSuccessResponse, TSuccessVerification> result) {
        super(app, executionResult, null);
        if (result == null) {
            throw new IllegalStateException("Cannot verify failure: no operation was executed");
        }
        this.failureVerification = result.shouldFail();
    }

    public ThenFailureImpl<TSuccessResponse, TSuccessVerification> errorMessage(String expectedMessage) {
        failureVerification.errorMessage(expectedMessage);
        return this;
    }

    public ThenFailureImpl<TSuccessResponse, TSuccessVerification> fieldErrorMessage(
            String expectedField, String expectedMessage) {
        failureVerification.fieldErrorMessage(expectedField, expectedMessage);
        return this;
    }

    @Override
    public ThenFailureImpl<TSuccessResponse, TSuccessVerification> and() {
        return this;
    }
}





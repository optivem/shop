package com.mycompany.myshop.backend.support.core.scenario.then.steps;

import com.mycompany.myshop.backend.support.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.backend.support.core.shared.ErrorVerification;
import com.mycompany.myshop.backend.support.core.shared.ResponseVerification;
import com.mycompany.myshop.backend.support.core.shared.UseCaseResult;
import com.mycompany.myshop.backend.support.core.shared.VoidVerification;
import com.mycompany.myshop.backend.support.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.support.port.then.steps.ThenFailure;

/**
 * A rejected action. The two rejection shapes stay apart — {@link #errorMessage(String)} reads
 * {@code ProblemDetail.detail}, {@link #fieldErrorMessage(String, String)} reads {@code errors[]} —
 * so a test cannot assert the wrong one and pass against the generic validation string. See
 * {@link ErrorVerification}.
 */
public class ThenFailureImpl<R, V extends ResponseVerification<R>>
        extends BaseThenStep<Void, VoidVerification> implements ThenFailure {

    private final ErrorVerification failureVerification;

    public ThenFailureImpl(
            UseCaseDsl app, ExecutionResultContext executionResult, UseCaseResult<R, V> result) {
        super(app, executionResult, null);
        if (result == null) {
            throw new IllegalStateException("Cannot verify failure: no action was executed");
        }
        this.failureVerification = result.shouldFail();
    }

    @Override
    public ThenFailureImpl<R, V> errorMessage(String expectedMessage) {
        failureVerification.errorMessage(expectedMessage);
        return this;
    }

    @Override
    public ThenFailureImpl<R, V> fieldErrorMessage(String expectedField, String expectedMessage) {
        failureVerification.fieldErrorMessage(expectedField, expectedMessage);
        return this;
    }

    @Override
    public ThenFailureImpl<R, V> and() {
        return this;
    }
}

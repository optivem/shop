package com.mycompany.myshop.backend.testkit.dsl.core.scenario.then;

import com.mycompany.myshop.backend.testkit.dsl.core.scenario.ExecutionResult;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.then.steps.ThenFailureImpl;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.then.steps.ThenSuccessImpl;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.testkit.dsl.port.then.ThenResultStage;

/**
 * The outcome of the action the scenario ran. A test must commit to one side —
 * {@code shouldSucceed()} or {@code shouldFail()} — before it can assert anything about it.
 */
public class ThenResultImpl<R, V extends ResponseVerification<R>> extends ThenImpl
        implements ThenResultStage {

    private final ExecutionResult<R, V> executionResult;

    public ThenResultImpl(UseCaseDsl app, ExecutionResult<R, V> executionResult) {
        super(app);
        this.executionResult = executionResult;
    }

    @Override
    public ThenSuccessImpl<R, V> shouldSucceed() {
        var successVerification = executionResult.getResult().shouldSucceed();
        return new ThenSuccessImpl<>(app, executionResult.getContext(), successVerification);
    }

    @Override
    public ThenFailureImpl<R, V> shouldFail() {
        return new ThenFailureImpl<>(
            app, executionResult.getContext(), executionResult.getResult());
    }
}

package com.mycompany.myshop.testkit.dsl.core.scenario.then;

import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResult;
import com.mycompany.myshop.testkit.dsl.core.scenario.then.steps.ThenFailureImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.then.steps.ThenSuccessImpl;
import com.mycompany.myshop.testkit.dsl.port.then.ThenResultStage;

import java.time.Duration;

public class ThenResultImpl<R, V extends ResponseVerification<R>> extends ThenImpl implements ThenResultStage {
    private final ExecutionResult<R, V> executionResult;
    /** How long the When step took to run, for asserting that a failure was fast. */
    private final Duration elapsed;

    public ThenResultImpl(UseCaseDsl app, ExecutionResult<R, V> executionResult, Duration elapsed) {
        super(app);
        this.executionResult = executionResult;
        this.elapsed = elapsed;
    }

    @Override
    public ThenSuccessImpl<R, V> shouldSucceed() {
        var successVerification = executionResult.getResult().shouldSucceed();
        return new ThenSuccessImpl<>(app, executionResult.getContext(), successVerification);
    }

    @Override
    public ThenFailureImpl<R, V> shouldFail() {
        return new ThenFailureImpl<>(app, executionResult.getContext(), executionResult.getResult(), elapsed);
    }
}



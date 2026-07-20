package com.mycompany.myshop.backend.support.core.scenario.when.steps;

import com.mycompany.myshop.backend.support.core.ScenarioDslImpl;
import com.mycompany.myshop.backend.support.core.scenario.ExecutionResult;
import com.mycompany.myshop.backend.support.core.scenario.then.ThenResultImpl;
import com.mycompany.myshop.backend.support.core.shared.ResponseVerification;
import com.mycompany.myshop.backend.support.core.usecase.UseCaseDsl;

/**
 * {@code then()} is where the action actually runs. The outcome is captured into an {@link
 * ExecutionResult} — success payload or rejection — rather than asserted, so the test states its
 * expectation next.
 */
public abstract class BaseWhenStep<R, V extends ResponseVerification<R>> {

    private final UseCaseDsl app;
    private final ScenarioDslImpl scenario;

    protected BaseWhenStep(UseCaseDsl app, ScenarioDslImpl scenario) {
        this.app = app;
        this.scenario = scenario;
    }

    public ThenResultImpl<R, V> then() {
        scenario.markAsExecuted();
        var result = execute(app);
        return new ThenResultImpl<>(app, result);
    }

    protected abstract ExecutionResult<R, V> execute(UseCaseDsl app);
}

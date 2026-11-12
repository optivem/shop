package com.mycompany.myshop.testkit.dsl.core.scenario.when.steps;

import com.mycompany.myshop.testkit.dsl.core.ScenarioDslImpl;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResult;
import com.mycompany.myshop.testkit.dsl.core.scenario.then.ThenResultImpl;

import java.time.Duration;

public abstract class BaseWhenStep<R, V extends ResponseVerification<R>> {
    private final UseCaseDsl app;
    private final ScenarioDslImpl scenario;

    protected BaseWhenStep(UseCaseDsl app, ScenarioDslImpl scenario) {
        this.app = app;
        this.scenario = scenario;
    }

    public ThenResultImpl<R, V> then() {
        scenario.markAsExecuted();
        var startedAt = System.nanoTime();
        var result = execute(app);
        var elapsed = Duration.ofNanos(System.nanoTime() - startedAt);
        return new ThenResultImpl<>(app, result, elapsed);
    }

    protected abstract ExecutionResult<R, V> execute(UseCaseDsl app);
}




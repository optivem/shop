package com.optivem.eshop.dsl.core;

import com.optivem.eshop.dsl.core.usecase.UseCaseDsl;
import com.optivem.eshop.dsl.core.scenario.assume.AssumeImpl;
import com.optivem.eshop.dsl.port.ScenarioDsl;
import com.optivem.eshop.dsl.core.scenario.given.GivenImpl;
import com.optivem.eshop.dsl.core.scenario.when.WhenImpl;

public class ScenarioDslImpl implements ScenarioDsl {
    private final UseCaseDsl app;
    private boolean executed = false;

    public ScenarioDslImpl(UseCaseDsl app) {
        this.app = app;
    }

    public AssumeImpl assume() {
        return new AssumeImpl(app);
    }

    public GivenImpl given() {
        ensureNotExecuted();
        return new GivenImpl(app);
    }

    public WhenImpl when() {
        ensureNotExecuted();
        return new WhenImpl(app);
    }

    public void markAsExecuted() {
        this.executed = true;
    }

    private void ensureNotExecuted() {
        if (executed) {
            throw new IllegalStateException("Scenario has already been executed. " +
                    "Each test method should contain only ONE scenario execution (Given-When-Then). " +
                    "Split multiple scenarios into separate test methods.");
        }
    }
}


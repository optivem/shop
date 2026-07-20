package com.mycompany.myshop.backend.testkit.dsl.core.scenario.assume;

import com.mycompany.myshop.backend.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.testkit.dsl.port.assume.AssumeStage;
import com.mycompany.myshop.backend.testkit.dsl.port.assume.steps.AssumeRunning;

/**
 * {@code assume().myShop().shouldBeRunning()} resolves to the {@code GET /health} liveness probe;
 * the external probes resolve to a read-only admin call against each stub. See {@link AssumeStage}
 * for what they cover.
 */
public class AssumeImpl implements AssumeStage {

    private final UseCaseDsl app;

    public AssumeImpl(UseCaseDsl app) {
        this.app = app;
    }

    @Override
    public AssumeRunning myShop() {
        return () -> {
            app.myShop().goToMyShop().execute().shouldSucceed();
            return this;
        };
    }

    @Override
    public AssumeRunning erp() {
        return () -> {
            app.erp().goToErp().execute();
            return this;
        };
    }

    @Override
    public AssumeRunning tax() {
        return () -> {
            app.tax().goToTax().execute();
            return this;
        };
    }

    @Override
    public AssumeRunning clock() {
        return () -> {
            app.clock().goToClock().execute();
            return this;
        };
    }
}

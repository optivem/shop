package com.mycompany.myshop.backend.support.core.scenario.assume;

import com.mycompany.myshop.backend.support.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.support.port.assume.AssumeStage;
import com.mycompany.myshop.backend.support.port.assume.steps.AssumeRunning;

/** {@code assume().myShop().shouldBeRunning()} resolves to the {@code GET /health} liveness probe. */
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
}

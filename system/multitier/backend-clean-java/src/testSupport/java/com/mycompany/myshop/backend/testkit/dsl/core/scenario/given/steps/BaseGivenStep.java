package com.mycompany.myshop.backend.testkit.dsl.core.scenario.given.steps;

import com.mycompany.myshop.backend.testkit.dsl.core.scenario.given.GivenImpl;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.then.ThenImpl;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.when.WhenImpl;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.testkit.dsl.port.given.steps.base.GivenStep;

public abstract class BaseGivenStep implements GivenStep {

    private final GivenImpl given;

    protected BaseGivenStep(GivenImpl given) {
        this.given = given;
    }

    @Override
    public GivenImpl and() {
        return given;
    }

    @Override
    public WhenImpl when() {
        return given.when();
    }

    @Override
    public ThenImpl then() {
        return given.then();
    }

    public abstract void execute(UseCaseDsl app);
}

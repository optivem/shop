package com.optivem.eshop.dsl.core.scenario.given.steps;

import com.optivem.eshop.dsl.core.usecase.UseCaseDsl;
import com.optivem.eshop.dsl.core.scenario.given.GivenImpl;
import com.optivem.eshop.dsl.port.then.ThenStage;
import com.optivem.eshop.dsl.port.given.steps.base.GivenStep;
import com.optivem.eshop.dsl.core.scenario.when.WhenImpl;

public abstract class BaseGivenStep implements GivenStep {
    private final GivenImpl given;

    protected BaseGivenStep(GivenImpl given) {
        this.given = given;
    }

    public GivenImpl and() {
        return given;
    }

    public WhenImpl when() {
        return given.when();
    }

    public ThenStage then() {
        return given.then();
    }

    public abstract void execute(UseCaseDsl app);
}




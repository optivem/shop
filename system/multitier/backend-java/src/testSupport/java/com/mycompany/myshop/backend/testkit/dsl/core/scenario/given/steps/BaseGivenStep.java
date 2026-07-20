package com.mycompany.myshop.backend.support.core.scenario.given.steps;

import com.mycompany.myshop.backend.support.core.scenario.given.GivenImpl;
import com.mycompany.myshop.backend.support.core.scenario.then.ThenImpl;
import com.mycompany.myshop.backend.support.core.scenario.when.WhenImpl;
import com.mycompany.myshop.backend.support.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.support.port.given.steps.base.GivenStep;

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

    /** Translates this step into use case calls. Run by {@link GivenImpl} on the hop into when/then. */
    public abstract void execute(UseCaseDsl app);
}

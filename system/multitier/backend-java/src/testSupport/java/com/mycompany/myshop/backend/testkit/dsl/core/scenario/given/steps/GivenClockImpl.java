package com.mycompany.myshop.backend.support.core.scenario.given.steps;

import com.mycompany.myshop.backend.support.core.scenario.ScenarioDefaults;
import com.mycompany.myshop.backend.support.core.scenario.given.GivenImpl;
import com.mycompany.myshop.backend.support.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.support.port.given.steps.GivenClock;

public class GivenClockImpl extends BaseGivenStep implements GivenClock {

    private String time;

    public GivenClockImpl(GivenImpl given) {
        super(given);
        withTime(ScenarioDefaults.DEFAULT_TIME);
    }

    @Override
    public GivenClockImpl withTime() {
        return withTime(ScenarioDefaults.DEFAULT_TIME);
    }

    @Override
    public GivenClockImpl withTime(String time) {
        this.time = time;
        return this;
    }

    @Override
    public void execute(UseCaseDsl app) {
        app.clock().returnsTime().time(time).execute();
    }
}

package com.optivem.shop.testkit.dsl.core.scenario.given.steps;

import com.optivem.shop.testkit.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.testkit.dsl.core.scenario.ScenarioDefaults;
import com.optivem.shop.testkit.dsl.core.scenario.given.GivenImpl;
import com.optivem.shop.testkit.dsl.port.given.steps.GivenClock;

public class GivenClockImpl extends BaseGivenStep implements GivenClock {
    private String time;

    public GivenClockImpl(GivenImpl given) {
        super(given);
        withTime(ScenarioDefaults.DEFAULT_TIME);
    }

    public GivenClockImpl withTime() {
        return withTime(ScenarioDefaults.DEFAULT_TIME);
    }

    public GivenClockImpl withTime(String time) {
        this.time = time;
        return this;
    }

    public GivenClockImpl withWeekday() {
        return withTime(ScenarioDefaults.WEEKDAY_TIME);
    }

    public GivenClockImpl withWeekend() {
        return withTime(ScenarioDefaults.WEEKEND_TIME);
    }

    @Override
    public void execute(UseCaseDsl app) {
        app.clock().returnsTime()
            .time(time)
            .execute()
            .shouldSucceed();
    }
}



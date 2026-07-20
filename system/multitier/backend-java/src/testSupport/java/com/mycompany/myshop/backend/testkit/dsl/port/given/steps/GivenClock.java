package com.mycompany.myshop.backend.testkit.dsl.port.given.steps;

import com.mycompany.myshop.backend.testkit.dsl.port.given.steps.base.GivenStep;

public interface GivenClock extends GivenStep {
    GivenClock withTime();

    GivenClock withTime(String time);
}

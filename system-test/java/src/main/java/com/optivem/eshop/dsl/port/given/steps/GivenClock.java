package com.optivem.eshop.dsl.port.given.steps;

import com.optivem.eshop.dsl.port.given.steps.base.GivenStep;

public interface GivenClock extends GivenStep {
    GivenClock withTime(String time);
}


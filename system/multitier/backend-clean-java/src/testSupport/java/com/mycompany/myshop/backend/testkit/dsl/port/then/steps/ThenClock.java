package com.mycompany.myshop.backend.testkit.dsl.port.then.steps;

import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.base.ThenStep;

public interface ThenClock extends ThenStep<ThenClock> {
    ThenClock hasTime(String expectedTime);
}

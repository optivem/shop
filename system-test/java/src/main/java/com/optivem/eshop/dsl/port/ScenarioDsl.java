package com.optivem.eshop.dsl.port;

import com.optivem.eshop.dsl.port.assume.AssumeStage;
import com.optivem.eshop.dsl.port.given.GivenStage;
import com.optivem.eshop.dsl.port.when.WhenStage;

public interface ScenarioDsl {
    AssumeStage assume();

    GivenStage given();

    WhenStage when();
}
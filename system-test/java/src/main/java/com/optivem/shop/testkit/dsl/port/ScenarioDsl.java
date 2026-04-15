package com.optivem.shop.testkit.dsl.port;

import com.optivem.shop.testkit.dsl.port.assume.AssumeStage;
import com.optivem.shop.testkit.dsl.port.given.GivenStage;
import com.optivem.shop.testkit.dsl.port.when.WhenStage;

public interface ScenarioDsl {
    AssumeStage assume();

    GivenStage given();

    WhenStage when();
}
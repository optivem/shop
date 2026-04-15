package com.optivem.shop.testkit.dsl.port.then;

import com.optivem.shop.testkit.dsl.port.then.steps.ThenFailure;
import com.optivem.shop.testkit.dsl.port.then.steps.ThenSuccess;

public interface ThenResultStage extends ThenStage {
    ThenSuccess shouldSucceed();

    ThenFailure shouldFail();
}

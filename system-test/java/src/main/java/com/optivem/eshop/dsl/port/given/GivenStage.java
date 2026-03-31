package com.optivem.eshop.dsl.port.given;

import com.optivem.eshop.dsl.port.given.steps.GivenClock;
import com.optivem.eshop.dsl.port.given.steps.GivenOrder;
import com.optivem.eshop.dsl.port.given.steps.GivenProduct;
import com.optivem.eshop.dsl.port.then.ThenStage;
import com.optivem.eshop.dsl.port.when.WhenStage;

public interface GivenStage {
    GivenClock clock();

    GivenProduct product();

    GivenOrder order();

    WhenStage when();

    ThenStage then();
}



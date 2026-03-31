package com.optivem.eshop.dsl.port.then;

import com.optivem.eshop.dsl.port.then.steps.ThenClock;
import com.optivem.eshop.dsl.port.then.steps.ThenProduct;

public interface ThenStage {
    ThenClock clock();

    ThenProduct product(String skuAlias);
}



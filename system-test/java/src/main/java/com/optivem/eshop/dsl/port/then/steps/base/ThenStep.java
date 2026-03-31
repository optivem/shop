package com.optivem.eshop.dsl.port.then.steps.base;

import com.optivem.eshop.dsl.port.then.steps.ThenClock;
import com.optivem.eshop.dsl.port.then.steps.ThenOrder;
import com.optivem.eshop.dsl.port.then.steps.ThenProduct;
public interface ThenStep<TThen> {
    TThen and();

    ThenOrder order();

    ThenOrder order(String orderNumber);

    ThenClock clock();

    ThenProduct product(String skuAlias);
}


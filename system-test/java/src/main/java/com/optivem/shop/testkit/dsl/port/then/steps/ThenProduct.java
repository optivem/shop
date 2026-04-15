package com.optivem.shop.testkit.dsl.port.then.steps;

import com.optivem.shop.testkit.dsl.port.then.steps.base.ThenStep;

public interface ThenProduct extends ThenStep<ThenProduct> {
    ThenProduct hasSku(String sku);

    ThenProduct hasPrice(double price);
}

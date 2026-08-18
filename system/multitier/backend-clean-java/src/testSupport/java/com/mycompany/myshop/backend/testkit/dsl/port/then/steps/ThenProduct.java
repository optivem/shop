package com.mycompany.myshop.backend.testkit.dsl.port.then.steps;

import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.base.ThenStep;

public interface ThenProduct extends ThenStep<ThenProduct> {
    ThenProduct hasSku(String expectedSku);

    ThenProduct hasPrice(double expectedPrice);

    ThenProduct hasPrice(String expectedPrice);
}

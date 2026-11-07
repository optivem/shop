package com.mycompany.myshop.testkit.dsl.port.then.steps;

import com.mycompany.myshop.testkit.dsl.port.then.steps.base.ThenStep;

public interface ThenProduct extends ThenStep<ThenProduct> {
    ThenProduct hasSku(String sku);

    ThenProduct hasPrice(String price);

    ThenProduct hasTitle(String title);

    ThenProduct hasDescription(String description);

    ThenProduct hasCategory(String category);

    ThenProduct hasBrand(String brand);
}

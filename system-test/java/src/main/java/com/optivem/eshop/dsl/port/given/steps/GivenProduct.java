package com.optivem.eshop.dsl.port.given.steps;

import com.optivem.eshop.dsl.port.given.steps.base.GivenStep;

public interface GivenProduct extends GivenStep {
    GivenProduct withSku(String sku);

    GivenProduct withUnitPrice(String unitPrice);

    GivenProduct withUnitPrice(double unitPrice);
}

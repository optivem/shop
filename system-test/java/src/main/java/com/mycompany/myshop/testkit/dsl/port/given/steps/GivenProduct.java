package com.mycompany.myshop.testkit.dsl.port.given.steps;

import com.mycompany.myshop.testkit.dsl.port.given.steps.base.GivenStep;

public interface GivenProduct extends GivenStep {
    GivenProduct withSku(String sku);

    GivenProduct withUnitPrice(String unitPrice);

    GivenProduct withTitle(String title);

    GivenProduct withDescription(String description);

    GivenProduct withCategory(String category);

    GivenProduct withBrand(String brand);
}

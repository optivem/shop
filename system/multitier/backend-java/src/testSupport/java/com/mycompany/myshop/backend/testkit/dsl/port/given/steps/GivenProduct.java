package com.mycompany.myshop.backend.support.port.given.steps;

import com.mycompany.myshop.backend.support.port.given.steps.base.GivenStep;

/**
 * A product the ERP knows about — or, via {@link #doesNotExist()}, one it explicitly does not.
 *
 * <p>The ERP is a stub, so "absent" has to be programmed as deliberately as "present": leaving the
 * SKU unstubbed would lean on WireMock's default 404 and hide what the scenario depends on.
 */
public interface GivenProduct extends GivenStep {
    GivenProduct withSku(String sku);

    GivenProduct withUnitPrice(String unitPrice);

    GivenProduct withUnitPrice(double unitPrice);

    GivenProduct doesNotExist();
}

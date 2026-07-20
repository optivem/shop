package com.mycompany.myshop.backend.support.port.then.steps;

import com.mycompany.myshop.backend.support.port.then.steps.base.ThenStep;

/**
 * A product as the SUT sees it: read back through the SUT's production {@code ErpGateway} (real HTTP
 * to the ERP stub + real {@code ProductDetailsResponse} parse), not through a test-side stub client.
 * That backing is what lets these assertions catch a real field drift in the component stub — see
 * {@link com.mycompany.myshop.backend.support.port.then.ThenStage}.
 */
public interface ThenProduct extends ThenStep<ThenProduct> {
    ThenProduct hasSku(String expectedSku);

    ThenProduct hasPrice(double expectedPrice);
}

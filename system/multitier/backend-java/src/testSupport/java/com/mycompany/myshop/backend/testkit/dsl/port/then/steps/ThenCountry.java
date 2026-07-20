package com.mycompany.myshop.backend.support.port.then.steps;

import com.mycompany.myshop.backend.support.port.then.steps.base.ThenStep;

/**
 * A country's tax details as the SUT sees them: read back through the SUT's production {@code
 * TaxGateway}. See {@link ThenProduct} for why the read goes through the production gateway.
 */
public interface ThenCountry extends ThenStep<ThenCountry> {
    ThenCountry hasTaxRate(double expectedTaxRate);
}

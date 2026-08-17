package com.mycompany.myshop.backend.domain.gateways;

import com.mycompany.myshop.backend.domain.entities.TaxRate;
import com.mycompany.myshop.backend.domain.values.Country;

import java.util.Optional;

/**
 * The domain's port to the tax system. Returns domain types only.
 */
public interface TaxGateway {

    /**
     * @return the country's tax rate, or empty when the tax system does not know the country.
     */
    Optional<TaxRate> getTaxDetails(Country country);
}

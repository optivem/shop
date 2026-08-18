package com.mycompany.myshop.backend.domain.gateways;

import com.mycompany.myshop.backend.domain.entities.TaxRate;
import com.mycompany.myshop.backend.domain.values.Country;

import java.util.Optional;

public interface TaxGateway {

    Optional<TaxRate> getTaxDetails(Country country);
}

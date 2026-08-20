package com.mycompany.myshop.backend.testkit.driver.adapter.sut;

import com.mycompany.myshop.backend.domain.gateways.TaxGateway;
import com.mycompany.myshop.backend.domain.values.Country;
import com.mycompany.myshop.backend.domain.values.TaxRate;
import java.util.Optional;

public class SutTaxReader {

    private final TaxGateway gateway;

    public SutTaxReader(TaxGateway gateway) {
        this.gateway = gateway;
    }

    public Optional<TaxRate> readCountry(String code) {
        return gateway.getTaxDetails(Country.of(code));
    }
}

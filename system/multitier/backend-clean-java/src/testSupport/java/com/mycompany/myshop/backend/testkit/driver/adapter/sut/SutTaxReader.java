package com.mycompany.myshop.backend.testkit.driver.adapter.sut;

import com.mycompany.myshop.backend.infrastructure.external.tax.TaxDetailsResponse;
import com.mycompany.myshop.backend.infrastructure.external.tax.HttpTaxGateway;
import java.util.Optional;

public class SutTaxReader {

    private final HttpTaxGateway gateway;

    public SutTaxReader(HttpTaxGateway gateway) {
        this.gateway = gateway;
    }

    public Optional<TaxDetailsResponse> readCountry(String code) {
        return gateway.fetchTaxDetails(code);
    }
}

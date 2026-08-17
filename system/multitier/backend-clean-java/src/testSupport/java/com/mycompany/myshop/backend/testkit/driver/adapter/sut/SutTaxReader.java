package com.mycompany.myshop.backend.testkit.driver.adapter.sut;

import com.mycompany.myshop.backend.infrastructure.external.tax.TaxDetailsResponse;
import com.mycompany.myshop.backend.infrastructure.external.tax.HttpTaxGateway;
import java.util.Optional;

/**
 * Reads a country's tax details AS THE SUT SEES IT: a real HTTP call to the (stubbed) Tax URL plus
 * the SUT's own {@link TaxDetailsResponse} parse, delegating to the production {@link HttpTaxGateway}.
 * See {@link SutErpReader} for why the read goes through the production gateway rather than a
 * test-side stub client.
 */
public class SutTaxReader {

    private final HttpTaxGateway gateway;

    public SutTaxReader(HttpTaxGateway gateway) {
        this.gateway = gateway;
    }

    public Optional<TaxDetailsResponse> readCountry(String code) {
        return gateway.fetchTaxDetails(code);
    }
}

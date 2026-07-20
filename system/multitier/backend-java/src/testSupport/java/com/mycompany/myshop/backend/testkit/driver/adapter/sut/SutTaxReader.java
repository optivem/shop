package com.mycompany.myshop.backend.support.harness;

import com.mycompany.myshop.backend.core.dtos.external.TaxDetailsResponse;
import com.mycompany.myshop.backend.core.services.external.TaxGateway;
import java.util.Optional;

/**
 * Reads a country's tax details AS THE SUT SEES IT: a real HTTP call to the (stubbed) Tax URL plus
 * the SUT's own {@link TaxDetailsResponse} parse, delegating to the production {@link TaxGateway}.
 * See {@link SutErpReader} for why the read goes through the production gateway rather than a
 * test-side stub client.
 */
public class SutTaxReader {

    private final TaxGateway gateway;

    public SutTaxReader(TaxGateway gateway) {
        this.gateway = gateway;
    }

    public Optional<TaxDetailsResponse> readCountry(String code) {
        return gateway.getTaxDetails(code);
    }
}

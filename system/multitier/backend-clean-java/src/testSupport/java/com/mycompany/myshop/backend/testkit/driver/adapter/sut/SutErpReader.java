package com.mycompany.myshop.backend.testkit.driver.adapter.sut;

import com.mycompany.myshop.backend.infrastructure.external.erp.ProductDetailsResponse;
import com.mycompany.myshop.backend.infrastructure.external.erp.HttpErpGateway;
import java.util.Optional;

/**
 * Reads a product AS THE SUT SEES IT: a real HTTP call to the (stubbed) ERP URL plus the SUT's own
 * {@link ProductDetailsResponse} parse, delegating to the production {@link HttpErpGateway}. Used by the
 * stub-contract component tests so the stub's bytes travel through the SUT's real gateway rather than
 * a test-side client — which is what lets the assertion fail on a real field drift, instead of
 * re-asserting the value the test just planted.
 */
public class SutErpReader {

    private final HttpErpGateway gateway;

    public SutErpReader(HttpErpGateway gateway) {
        this.gateway = gateway;
    }

    public Optional<ProductDetailsResponse> readProduct(String sku) {
        return gateway.fetchProductDetails(sku);
    }
}

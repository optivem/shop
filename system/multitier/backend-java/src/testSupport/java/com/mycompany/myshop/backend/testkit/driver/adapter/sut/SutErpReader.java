package com.mycompany.myshop.backend.testkit.driver.adapter.sut;

import com.mycompany.myshop.backend.core.dtos.external.ErpProductDetailsResponse;
import com.mycompany.myshop.backend.core.services.external.ErpGateway;
import java.util.Optional;

/**
 * Reads a product AS THE SUT SEES IT: a real HTTP call to the (stubbed) ERP URL plus the SUT's own
 * {@link ErpProductDetailsResponse} parse, delegating to the production {@link ErpGateway}. Used by the
 * stub-contract component tests so the stub's bytes travel through the SUT's real gateway rather than
 * a test-side client — which is what lets the assertion fail on a real field drift, instead of
 * re-asserting the value the test just planted.
 */
public class SutErpReader {

    private final ErpGateway gateway;

    public SutErpReader(ErpGateway gateway) {
        this.gateway = gateway;
    }

    public Optional<ErpProductDetailsResponse> readProduct(String sku) {
        return gateway.getProductDetails(sku);
    }
}

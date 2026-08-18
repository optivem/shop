package com.mycompany.myshop.backend.testkit.driver.adapter.sut;

import com.mycompany.myshop.backend.infrastructure.external.erp.ProductDetailsResponse;
import com.mycompany.myshop.backend.infrastructure.external.erp.HttpErpGateway;
import java.util.Optional;

public class SutErpReader {

    private final HttpErpGateway gateway;

    public SutErpReader(HttpErpGateway gateway) {
        this.gateway = gateway;
    }

    public Optional<ProductDetailsResponse> readProduct(String sku) {
        return gateway.fetchProductDetails(sku);
    }
}

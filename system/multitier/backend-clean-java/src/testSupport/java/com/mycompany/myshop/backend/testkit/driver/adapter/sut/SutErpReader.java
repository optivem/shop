package com.mycompany.myshop.backend.testkit.driver.adapter.sut;

import com.mycompany.myshop.backend.domain.gateways.ErpGateway;
import com.mycompany.myshop.backend.domain.values.Product;
import com.mycompany.myshop.backend.domain.values.Sku;
import java.util.Optional;

public class SutErpReader {

    private final ErpGateway gateway;

    public SutErpReader(ErpGateway gateway) {
        this.gateway = gateway;
    }

    public Optional<Product> readProduct(String sku) {
        return gateway.getProductDetails(Sku.of(sku));
    }
}

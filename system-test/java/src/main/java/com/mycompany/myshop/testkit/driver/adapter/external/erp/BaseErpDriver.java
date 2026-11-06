package com.mycompany.myshop.testkit.driver.adapter.external.erp;

import com.mycompany.myshop.testkit.driver.port.external.erp.ErpDriver;

import com.mycompany.myshop.testkit.driver.adapter.external.erp.client.BaseErpClient;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.GetProductRequest;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.GetProductResponse;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.GetProductsResponse;
import com.mycompany.myshop.testkit.driver.adapter.external.erp.client.dtos.ExtProductDetailsResponse;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.error.ErpErrorResponse;
import com.mycompany.myshop.testkit.common.Closer;
import com.mycompany.myshop.testkit.common.Result;

public abstract class BaseErpDriver<C extends BaseErpClient> implements ErpDriver {
    protected final C client;

    protected BaseErpDriver(C client) {
        this.client = client;
    }

    @Override
    public void close() throws Exception {
        Closer.close(client);
    }

    @Override
    public Result<Void, ErpErrorResponse> goToErp() {
        return client.checkHealth()
                .mapError(ext -> new ErpErrorResponse(ext.getMessage()));
    }

    @Override
    public Result<GetProductResponse, ErpErrorResponse> getProduct(GetProductRequest request) {
        return client.getProduct(request.getSku())
                .map(productDetails -> GetProductResponse.builder()
                        .sku(productDetails.getId())
                        .price(productDetails.getPrice())
                        .build())
                .mapError(ext -> new ErpErrorResponse(ext.getMessage()));
    }

    @Override
    public Result<GetProductsResponse, ErpErrorResponse> getProducts() {
        return client.getProducts()
                .map(products -> GetProductsResponse.builder()
                        .products(products.stream().map(BaseErpDriver::toGetProductResponse).toList())
                        .build())
                .mapError(ext -> new ErpErrorResponse(ext.getMessage()));
    }

    private static GetProductResponse toGetProductResponse(ExtProductDetailsResponse productDetails) {
        return GetProductResponse.builder()
                .sku(productDetails.getId())
                .price(productDetails.getPrice())
                .build();
    }
}

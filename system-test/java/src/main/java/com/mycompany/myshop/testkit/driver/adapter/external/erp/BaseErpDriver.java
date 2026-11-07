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
    protected static final String DEFAULT_TITLE = "Test Product Title";
    protected static final String DEFAULT_DESCRIPTION = "Test Product Description";
    protected static final String DEFAULT_CATEGORY = "Test Category";
    protected static final String DEFAULT_BRAND = "Test Brand";

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
                .map(BaseErpDriver::toGetProductResponse)
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
                .title(productDetails.getTitle())
                .description(productDetails.getDescription())
                .category(productDetails.getCategory())
                .brand(productDetails.getBrand())
                .build();
    }

    /** Fields a scenario leaves unset fall back to the same defaults in the real ERP and the stub. */
    protected static String orDefault(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }
}

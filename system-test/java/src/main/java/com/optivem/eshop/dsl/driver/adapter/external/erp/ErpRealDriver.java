package com.optivem.eshop.dsl.driver.adapter.external.erp;

import com.optivem.eshop.dsl.driver.adapter.external.erp.client.ErpRealClient;
import com.optivem.eshop.dsl.driver.adapter.external.erp.client.dtos.ExtCreateProductRequest;
import com.optivem.eshop.dsl.driver.port.external.erp.dtos.ReturnsProductRequest;
import com.optivem.eshop.dsl.driver.port.shared.dtos.ErrorResponse;
import com.optivem.eshop.dsl.common.Result;

public class ErpRealDriver extends BaseErpDriver<ErpRealClient> {
    private static final String DEFAULT_TITLE = "Test Product Title";
    private static final String DEFAULT_DESCRIPTION = "Test Product Description";
    private static final String DEFAULT_CATEGORY = "Test Category";
    private static final String DEFAULT_BRAND = "Test Brand";

    public ErpRealDriver(String baseUrl) {
        super(new ErpRealClient(baseUrl));
    }

    @Override
    public Result<Void, ErrorResponse> returnsProduct(ReturnsProductRequest request) {
        var createProductRequest = ExtCreateProductRequest.builder()
                .id(request.getSku())
                .title(DEFAULT_TITLE)
                .description(DEFAULT_DESCRIPTION)
                .category(DEFAULT_CATEGORY)
                .brand(DEFAULT_BRAND)
                .price(request.getPrice())
                .build();

        return client.createProduct(createProductRequest)
                .mapError(ext -> ErrorResponse.builder().message(ext.getMessage()).build());
    }
}

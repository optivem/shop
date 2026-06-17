package com.mycompany.myshop.testkit.driver.adapter.external.erp;

import com.mycompany.myshop.testkit.driver.adapter.external.erp.client.ErpRealClient;
import com.mycompany.myshop.testkit.driver.adapter.external.erp.client.dtos.ExtCreateProductRequest;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.ReturnsProductRequest;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.ReturnsPromotionRequest;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.error.ErpErrorResponse;
import com.mycompany.myshop.testkit.common.Result;

public class ErpRealDriver extends BaseErpDriver<ErpRealClient> {
    private static final String DEFAULT_TITLE = "Test Product Title";
    private static final String DEFAULT_DESCRIPTION = "Test Product Description";
    private static final String DEFAULT_CATEGORY = "Test Category";
    private static final String DEFAULT_BRAND = "Test Brand";

    public ErpRealDriver(String baseUrl) {
        super(new ErpRealClient(baseUrl));
    }

    @Override
    public Result<Void, ErpErrorResponse> returnsPromotion(ReturnsPromotionRequest request) {
        return Result.success();
    }

    @Override
    public Result<Void, ErpErrorResponse> returnsProduct(ReturnsProductRequest request) {
        var createProductRequest = ExtCreateProductRequest.builder()
                .id(request.getSku())
                .title(DEFAULT_TITLE)
                .description(DEFAULT_DESCRIPTION)
                .category(DEFAULT_CATEGORY)
                .brand(DEFAULT_BRAND)
                .price(request.getPrice())
                .weight(request.getWeight())
                .build();

        return client.createProduct(createProductRequest)
                .mapError(ext -> new ErpErrorResponse(ext.getMessage()));
    }
}

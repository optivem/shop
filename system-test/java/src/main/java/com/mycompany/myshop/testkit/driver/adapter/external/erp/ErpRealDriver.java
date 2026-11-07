package com.mycompany.myshop.testkit.driver.adapter.external.erp;

import com.mycompany.myshop.testkit.driver.adapter.external.erp.client.ErpRealClient;
import com.mycompany.myshop.testkit.driver.adapter.external.erp.client.dtos.ExtCreateProductRequest;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.ReturnsProductRequest;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.ReturnsProductsRequest;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.ReturnsPromotionRequest;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.error.ErpErrorResponse;
import com.mycompany.myshop.testkit.common.Result;

public class ErpRealDriver extends BaseErpDriver<ErpRealClient> {
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
                .title(orDefault(request.getTitle(), DEFAULT_TITLE))
                .description(orDefault(request.getDescription(), DEFAULT_DESCRIPTION))
                .category(orDefault(request.getCategory(), DEFAULT_CATEGORY))
                .brand(orDefault(request.getBrand(), DEFAULT_BRAND))
                .price(request.getPrice())
                .build();

        return client.createProduct(createProductRequest)
                .mapError(ext -> new ErpErrorResponse(ext.getMessage()));
    }

    /** The real ERP lists every product it holds, so staging a list means creating each product in it. */
    @Override
    public Result<Void, ErpErrorResponse> returnsProducts(ReturnsProductsRequest request) {
        for (var product : request.getProducts()) {
            var result = returnsProduct(product);
            if (result.isFailure()) {
                return result;
            }
        }

        return Result.success();
    }

    /**
     * The real ERP cannot be taken down on demand, so this throws rather than silently succeeding,
     * which would let an ERP-unavailable test pass for the wrong reason.
     */
    @Override
    public Result<Void, ErpErrorResponse> returnsUnavailable() {
        throw new UnsupportedOperationException(
                "ErpRealDriver cannot make the ERP unavailable; run this scenario against the ERP stub");
    }
}

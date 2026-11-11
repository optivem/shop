package com.mycompany.myshop.testkit.driver.adapter.external.erp;

import com.mycompany.myshop.testkit.driver.adapter.external.erp.client.ErpStubClient;
import com.mycompany.myshop.testkit.driver.adapter.external.erp.client.dtos.ExtGetPromotionResponse;
import com.mycompany.myshop.testkit.driver.adapter.external.erp.client.dtos.ExtProductDetailsResponse;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.ReturnsProductRequest;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.ReturnsProductsRequest;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.ReturnsPromotionRequest;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.error.ErpErrorResponse;
import com.mycompany.myshop.testkit.common.Converter;
import com.mycompany.myshop.testkit.common.Result;

/**
 * ErpStubDriver uses WireMock to stub ERP API responses.
 * This allows tests to run without a real ERP system.
 */
public class ErpStubDriver extends BaseErpDriver<ErpStubClient> {
    public ErpStubDriver(String baseUrl) {
        super(new ErpStubClient(baseUrl));
    }

    @Override
    public void close() throws Exception {
        client.removeStubs();
        super.close();
    }

    @Override
    public Result<Void, ErpErrorResponse> returnsProduct(ReturnsProductRequest request) {
        return client.configureGetProduct(toExtProductDetailsResponse(request))
                .mapError(ext -> new ErpErrorResponse(ext.getMessage()));
    }

    @Override
    public Result<Void, ErpErrorResponse> returnsProducts(ReturnsProductsRequest request) {
        var extProducts = request.getProducts().stream()
                .map(ErpStubDriver::toExtProductDetailsResponse)
                .toList();

        // The ERP serves each listed product on its own as well (GET /api/products/{sku}), as the real ERP does.
        for (var extProduct : extProducts) {
            var result = client.configureGetProduct(extProduct);
            if (result.isFailure()) {
                return result.mapError(ext -> new ErpErrorResponse(ext.getMessage()));
            }
        }
        return client.configureGetProducts(extProducts)
                .mapError(ext -> new ErpErrorResponse(ext.getMessage()));
    }

    private static ExtProductDetailsResponse toExtProductDetailsResponse(ReturnsProductRequest request) {
        return ExtProductDetailsResponse.builder()
                .id(request.getSku())
                .title(orDefault(request.getTitle(), DEFAULT_TITLE))
                .description(orDefault(request.getDescription(), DEFAULT_DESCRIPTION))
                .price(Converter.toBigDecimal(request.getPrice()))
                .category(orDefault(request.getCategory(), DEFAULT_CATEGORY))
                .brand(orDefault(request.getBrand(), DEFAULT_BRAND))
                .build();
    }

    @Override
    public Result<Void, ErpErrorResponse> returnsPromotion(ReturnsPromotionRequest request) {
        var extPromotionResponse = ExtGetPromotionResponse.builder()
                .promotionActive(request.isPromotionActive())
                .discount(Converter.toBigDecimal(request.getDiscount()))
                .build();

        return client.configureGetPromotion(extPromotionResponse)
                .mapError(ext -> new ErpErrorResponse(ext.getMessage()));
    }

    @Override
    public Result<Void, ErpErrorResponse> returnsUnavailable() {
        return client.configureUnavailable()
                .mapError(ext -> new ErpErrorResponse(ext.getMessage()));
    }
}

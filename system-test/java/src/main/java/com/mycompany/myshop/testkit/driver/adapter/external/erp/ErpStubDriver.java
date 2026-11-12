package com.mycompany.myshop.testkit.driver.adapter.external.erp;

import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.GetProductRequest;
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
    /**
     * How long to wait once an ERP outage (unavailable or slow) ends. The system's ERP circuit breaker may have
     * opened during the outage, or reopened on its last slow call just before the test got its answer; until its
     * open time passes (100-500 ms on the stub stacks) it still turns ERP calls away with 503, so without the wait
     * the outage would leak into the next step or test.
     */
    private static final long OUTAGE_RECOVERY_MILLISECONDS = 1_000;

    private boolean outage;

    public ErpStubDriver(String baseUrl) {
        super(new ErpStubClient(baseUrl));
    }

    @Override
    public void close() throws Exception {
        client.removeStubs();
        awaitRecoveryFromOutage();
        super.close();
    }

    private void awaitRecoveryFromOutage() throws InterruptedException {
        if (outage) {
            outage = false;
            Thread.sleep(OUTAGE_RECOVERY_MILLISECONDS);
        }
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
        outage = true;
        return client.configureUnavailable()
                .mapError(ext -> new ErpErrorResponse(ext.getMessage()));
    }

    @Override
    public Result<Void, ErpErrorResponse> returnsSlow() {
        outage = true;
        return client.configureSlow()
                .mapError(ext -> new ErpErrorResponse(ext.getMessage()));
    }

    @Override
    public Result<Void, ErpErrorResponse> returnsProductNotFound(GetProductRequest request) {
        return client.configureGetProductNotFound(request.getSku())
                .mapError(ext -> new ErpErrorResponse(ext.getMessage()));
    }

    @Override
    public Result<Void, ErpErrorResponse> returnsAvailable() {
        var result = client.removeUnavailable()
                .mapError(ext -> new ErpErrorResponse(ext.getMessage()));
        try {
            awaitRecoveryFromOutage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the system to recover from the ERP outage", e);
        }
        return result;
    }
}

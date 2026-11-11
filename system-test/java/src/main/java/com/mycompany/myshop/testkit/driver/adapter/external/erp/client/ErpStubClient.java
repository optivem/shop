package com.mycompany.myshop.testkit.driver.adapter.external.erp.client;

import com.mycompany.myshop.testkit.driver.adapter.shared.client.http.HttpStatus;
import com.mycompany.myshop.testkit.driver.adapter.external.erp.client.dtos.ExtGetPromotionResponse;
import com.mycompany.myshop.testkit.driver.adapter.external.erp.client.dtos.ExtProductDetailsResponse;
import com.mycompany.myshop.testkit.driver.adapter.external.erp.client.dtos.error.ExtErpErrorResponse;
import com.mycompany.myshop.testkit.common.Result;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.wiremock.JsonWireMockClient;

import java.util.List;

public class ErpStubClient extends BaseErpClient {
    private static final String ERP_PRODUCTS_ENDPOINT = "/erp/api/products";
    private static final String ERP_PROMOTION_ENDPOINT = "/erp/api/promotion";
    private static final String ERP_API_PATTERN = "/erp/api/.*";
    private static final int OUTAGE_PRIORITY = 1;

    private final JsonWireMockClient wireMockClient;

    public ErpStubClient(String baseUrl) {
        super(baseUrl);
        this.wireMockClient = new JsonWireMockClient(baseUrl);
    }

    public Result<Void, ExtErpErrorResponse> configureGetProduct(ExtProductDetailsResponse response) {
        var sku = response.getId();
        return wireMockClient.stubGet(ERP_PRODUCTS_ENDPOINT + "/" + sku, HttpStatus.OK, response)
                .mapError(ExtErpErrorResponse::new);
    }

    /** Asking for this product answers 404; the newest stub wins over the product stubbed before. */
    public Result<Void, ExtErpErrorResponse> configureGetProductNotFound(String sku) {
        return wireMockClient.stubGet(ERP_PRODUCTS_ENDPOINT + "/" + sku, HttpStatus.NOT_FOUND)
                .mapError(ExtErpErrorResponse::new);
    }

    public Result<Void, ExtErpErrorResponse> configureGetProducts(List<ExtProductDetailsResponse> response) {
        return wireMockClient.stubGet(ERP_PRODUCTS_ENDPOINT, HttpStatus.OK, response)
                .mapError(ExtErpErrorResponse::new);
    }

    public Result<Void, ExtErpErrorResponse> configureGetPromotion(ExtGetPromotionResponse response) {
        return wireMockClient.stubGet(ERP_PROMOTION_ENDPOINT, HttpStatus.OK, response)
                .mapError(ExtErpErrorResponse::new);
    }

    /** Every ERP API call answers 503, outranking any product or promotion stubs. */
    public Result<Void, ExtErpErrorResponse> configureUnavailable() {
        return wireMockClient.stubGetMatching(ERP_API_PATTERN, HttpStatus.SERVICE_UNAVAILABLE, OUTAGE_PRIORITY)
                .mapError(ExtErpErrorResponse::new);
    }

    /** Removes the outage stub, so the product and promotion stubs answer again. */
    public Result<Void, ExtErpErrorResponse> removeUnavailable() {
        return wireMockClient.removeStubsMatching(ERP_API_PATTERN)
                .mapError(ExtErpErrorResponse::new);
    }

    public void removeStubs() {
        wireMockClient.removeStubs();
    }
}

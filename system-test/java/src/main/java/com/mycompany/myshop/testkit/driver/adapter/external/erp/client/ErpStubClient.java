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

    public Result<Void, ExtErpErrorResponse> configureGetProducts(List<ExtProductDetailsResponse> response) {
        return wireMockClient.stubGet(ERP_PRODUCTS_ENDPOINT, HttpStatus.OK, response)
                .mapError(ExtErpErrorResponse::new);
    }

    public Result<Void, ExtErpErrorResponse> configureGetPromotion(ExtGetPromotionResponse response) {
        return wireMockClient.stubGet(ERP_PROMOTION_ENDPOINT, HttpStatus.OK, response)
                .mapError(ExtErpErrorResponse::new);
    }

    public void removeStubs() {
        wireMockClient.removeStubs();
    }
}

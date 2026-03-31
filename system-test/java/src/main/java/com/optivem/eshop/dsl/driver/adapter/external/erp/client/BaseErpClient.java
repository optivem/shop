package com.optivem.eshop.dsl.driver.adapter.external.erp.client;

import com.optivem.eshop.dsl.driver.adapter.external.erp.client.dtos.ExtProductDetailsResponse;
import com.optivem.eshop.dsl.driver.adapter.external.erp.client.dtos.error.ExtErpErrorResponse;
import com.optivem.eshop.dsl.driver.adapter.shared.client.http.JsonHttpClient;
import com.optivem.eshop.dsl.common.Closer;
import com.optivem.eshop.dsl.common.Result;

public abstract class BaseErpClient implements AutoCloseable {
    private static final String HEALTH_ENDPOINT = "/health";
    private static final String PRODUCTS_ENDPOINT = "/api/products";

    protected final JsonHttpClient<ExtErpErrorResponse> httpClient;

    protected BaseErpClient(String baseUrl) {
        this.httpClient = new JsonHttpClient<>(baseUrl, ExtErpErrorResponse.class);
    }

    @Override
    public void close() {
        Closer.close(httpClient);
    }

    public Result<Void, ExtErpErrorResponse> checkHealth() {
        return httpClient.get(HEALTH_ENDPOINT);
    }

    public Result<ExtProductDetailsResponse, ExtErpErrorResponse> getProduct(String sku) {
        return httpClient.get(PRODUCTS_ENDPOINT + "/" + sku, ExtProductDetailsResponse.class);
    }
}

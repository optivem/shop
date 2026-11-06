package com.mycompany.myshop.testkit.driver.adapter.external.erp.client;

import com.mycompany.myshop.testkit.driver.adapter.external.erp.client.dtos.ExtProductDetailsResponse;
import com.mycompany.myshop.testkit.driver.adapter.external.erp.client.dtos.error.ExtErpErrorResponse;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.http.JsonHttpClient;
import com.mycompany.myshop.testkit.common.Closer;
import com.mycompany.myshop.testkit.common.Result;

import java.util.List;

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

    public Result<List<ExtProductDetailsResponse>, ExtErpErrorResponse> getProducts() {
        return httpClient.get(PRODUCTS_ENDPOINT, ExtProductDetailsResponse[].class)
                .map(List::of);
    }
}

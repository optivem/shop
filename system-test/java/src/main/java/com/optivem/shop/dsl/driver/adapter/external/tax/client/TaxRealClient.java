package com.optivem.shop.dsl.driver.adapter.external.tax.client;

import com.optivem.shop.dsl.common.Closer;
import com.optivem.shop.dsl.common.Result;
import com.optivem.shop.dsl.driver.adapter.external.tax.client.dtos.ExtGetCountryResponse;
import com.optivem.shop.dsl.driver.adapter.external.tax.client.dtos.error.ExtTaxErrorResponse;
import com.optivem.shop.dsl.driver.adapter.shared.client.http.JsonHttpClient;

public class TaxRealClient implements AutoCloseable {
    private static final String HEALTH_ENDPOINT = "/health";
    private static final String COUNTRIES_ENDPOINT = "/api/countries/";

    private final JsonHttpClient<ExtTaxErrorResponse> httpClient;

    public TaxRealClient(String baseUrl) {
        this.httpClient = new JsonHttpClient<>(baseUrl, ExtTaxErrorResponse.class);
    }

    @Override
    public void close() {
        Closer.close(httpClient);
    }

    public Result<Void, ExtTaxErrorResponse> checkHealth() {
        return httpClient.get(HEALTH_ENDPOINT);
    }

    public Result<ExtGetCountryResponse, ExtTaxErrorResponse> getCountry(String country) {
        return httpClient.get(COUNTRIES_ENDPOINT + country, ExtGetCountryResponse.class);
    }
}

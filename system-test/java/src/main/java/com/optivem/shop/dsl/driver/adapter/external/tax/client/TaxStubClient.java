package com.optivem.shop.dsl.driver.adapter.external.tax.client;

import com.optivem.shop.dsl.common.Closer;
import com.optivem.shop.dsl.common.Result;
import com.optivem.shop.dsl.driver.adapter.external.tax.client.dtos.ExtGetCountryResponse;
import com.optivem.shop.dsl.driver.adapter.external.tax.client.dtos.error.ExtTaxErrorResponse;
import com.optivem.shop.dsl.driver.adapter.shared.client.http.HttpStatus;
import com.optivem.shop.dsl.driver.adapter.shared.client.http.JsonHttpClient;
import com.optivem.shop.dsl.driver.adapter.shared.client.wiremock.JsonWireMockClient;

public class TaxStubClient implements AutoCloseable {
    private static final String HEALTH_ENDPOINT = "/tax/health";
    private static final String COUNTRIES_ENDPOINT = "/tax/api/countries/";

    private final JsonHttpClient<ExtTaxErrorResponse> httpClient;
    private final JsonWireMockClient wireMockClient;

    public TaxStubClient(String baseUrl) {
        this.httpClient = new JsonHttpClient<>(baseUrl, ExtTaxErrorResponse.class);
        this.wireMockClient = new JsonWireMockClient(baseUrl);
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

    public Result<Void, ExtTaxErrorResponse> configureGetCountry(String country, ExtGetCountryResponse response) {
        return wireMockClient.stubGet(COUNTRIES_ENDPOINT + country, HttpStatus.OK, response)
                .mapError(ExtTaxErrorResponse::new);
    }

    public void removeStubs() {
        wireMockClient.removeStubs();
    }
}

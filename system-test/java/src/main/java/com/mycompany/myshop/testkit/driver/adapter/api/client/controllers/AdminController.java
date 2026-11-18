package com.mycompany.myshop.testkit.driver.adapter.api.client.controllers;

import com.mycompany.myshop.testkit.driver.adapter.api.client.dtos.errors.ProblemDetailResponse;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.http.JsonHttpClient;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCustomersResponse;
import com.mycompany.myshop.testkit.common.Result;

public class AdminController {
    private static final String ENDPOINT = "/api/admin";

    private final JsonHttpClient<ProblemDetailResponse> httpClient;

    public AdminController(JsonHttpClient<ProblemDetailResponse> httpClient) {
        this.httpClient = httpClient;
    }

    public Result<BrowseCustomersResponse, ProblemDetailResponse> browseCustomers() {
        return httpClient.get(ENDPOINT + "/customers", BrowseCustomersResponse.class);
    }
}

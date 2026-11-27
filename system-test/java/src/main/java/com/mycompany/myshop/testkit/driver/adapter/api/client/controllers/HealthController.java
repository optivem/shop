package com.mycompany.myshop.testkit.driver.adapter.api.client.controllers;

import com.mycompany.myshop.testkit.driver.adapter.api.client.dtos.errors.ProblemDetailResponse;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.http.JsonHttpClient;
import com.mycompany.myshop.testkit.common.Result;

public class HealthController {
    private static final String ENDPOINT = "/health";
    /** Readiness: answers 200 only when the system can serve requests, its database included. */
    private static final String READINESS_ENDPOINT = "/readyz";

    private final JsonHttpClient<ProblemDetailResponse> httpClient;

    public HealthController(JsonHttpClient<ProblemDetailResponse> httpClient) {
        this.httpClient = httpClient;
    }

    public Result<Void, ProblemDetailResponse> checkHealth() {
        return httpClient.get(ENDPOINT);
    }

    public Result<Void, ProblemDetailResponse> checkReadiness() {
        return httpClient.get(READINESS_ENDPOINT);
    }
}




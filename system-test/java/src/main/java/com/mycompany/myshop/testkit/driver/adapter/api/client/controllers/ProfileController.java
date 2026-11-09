package com.mycompany.myshop.testkit.driver.adapter.api.client.controllers;

import com.mycompany.myshop.testkit.common.Result;
import com.mycompany.myshop.testkit.driver.adapter.api.client.dtos.errors.ProblemDetailResponse;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.http.JsonHttpClient;
import com.mycompany.myshop.testkit.driver.port.dtos.UpdateProfileRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewProfileResponse;

public class ProfileController {
    private static final String ENDPOINT = "/api/account/profile";

    private final JsonHttpClient<ProblemDetailResponse> httpClient;

    public ProfileController(JsonHttpClient<ProblemDetailResponse> httpClient) {
        this.httpClient = httpClient;
    }

    public Result<ViewProfileResponse, ProblemDetailResponse> viewProfile() {
        return httpClient.get(ENDPOINT, ViewProfileResponse.class);
    }

    public Result<ViewProfileResponse, ProblemDetailResponse> updateProfile(UpdateProfileRequest request) {
        return httpClient.put(ENDPOINT, request, ViewProfileResponse.class);
    }
}

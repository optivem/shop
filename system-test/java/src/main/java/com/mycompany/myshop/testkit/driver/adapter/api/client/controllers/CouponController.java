package com.mycompany.myshop.testkit.driver.adapter.api.client.controllers;

import com.mycompany.myshop.testkit.driver.adapter.api.client.dtos.errors.ProblemDetailResponse;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.http.JsonHttpClient;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCouponsResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.PublishCouponRequest;
import com.mycompany.myshop.testkit.common.Result;
import java.util.LinkedHashMap;

public class CouponController {
    private static final String ENDPOINT = "/api/coupons";

    private final JsonHttpClient<ProblemDetailResponse> httpClient;

    public CouponController(JsonHttpClient<ProblemDetailResponse> httpClient) {
        this.httpClient = httpClient;
    }

    public Result<Void, ProblemDetailResponse> publishCoupon(PublishCouponRequest request) {
        return httpClient.post(ENDPOINT, request);
    }

    public Result<BrowseCouponsResponse, ProblemDetailResponse> browseCoupons(String page, String size) {
        var query = new LinkedHashMap<String, String>();
        query.put("page", page);
        query.put("size", size);
        return httpClient.get(ENDPOINT + QueryString.of(query), BrowseCouponsResponse.class);
    }
}

package com.optivem.shop.dsl.driver.adapter.shop.api.client.controllers;

import com.optivem.shop.dsl.common.Converter;
import com.optivem.shop.dsl.driver.adapter.shop.api.client.dtos.errors.ProblemDetailResponse;
import com.optivem.shop.dsl.driver.adapter.shared.client.http.JsonHttpClient;
import com.optivem.shop.dsl.driver.port.shop.dtos.BrowseCouponsResponse;
import com.optivem.shop.dsl.driver.port.shop.dtos.PublishCouponRequest;
import com.optivem.shop.dsl.common.Result;

import java.util.Map;

public class CouponController {
    private static final String ENDPOINT = "/api/coupons";

    private final JsonHttpClient<ProblemDetailResponse> httpClient;

    public CouponController(JsonHttpClient<ProblemDetailResponse> httpClient) {
        this.httpClient = httpClient;
    }

    public Result<Void, ProblemDetailResponse> publishCoupon(PublishCouponRequest request) {
        var body = Map.of(
                "code", request.getCode(),
                "discountRate", Converter.toBigDecimal(request.getDiscountRate())
        );
        return httpClient.post(ENDPOINT, body);
    }

    public Result<BrowseCouponsResponse, ProblemDetailResponse> browseCoupons() {
        return httpClient.get(ENDPOINT, BrowseCouponsResponse.class);
    }
}

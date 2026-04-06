package com.optivem.shop.dsl.driver.adapter.shop.api.client.controllers;

import com.optivem.shop.dsl.driver.port.shop.dtos.ViewOrderResponse;
import com.optivem.shop.dsl.driver.port.shop.dtos.PlaceOrderRequest;
import com.optivem.shop.dsl.driver.port.shop.dtos.PlaceOrderResponse;
import com.optivem.shop.dsl.driver.adapter.shop.api.client.dtos.errors.ProblemDetailResponse;
import com.optivem.shop.dsl.driver.adapter.shared.client.http.JsonHttpClient;
import com.optivem.shop.dsl.common.Result;

public class OrderController {
    private static final String ENDPOINT = "/api/orders";

    private final JsonHttpClient<ProblemDetailResponse> httpClient;

    public OrderController(JsonHttpClient<ProblemDetailResponse> httpClient) {
        this.httpClient = httpClient;
    }

    public Result<PlaceOrderResponse, ProblemDetailResponse> placeOrder(PlaceOrderRequest request) {
        return httpClient.post(ENDPOINT, request, PlaceOrderResponse.class);
    }

    public Result<ViewOrderResponse, ProblemDetailResponse> viewOrder(String orderNumber) {
        return httpClient.get(ENDPOINT + "/" + orderNumber, ViewOrderResponse.class);
    }

    public Result<Void, ProblemDetailResponse> cancelOrder(String orderNumber) {
        return httpClient.post(ENDPOINT + "/" + orderNumber + "/cancel");
    }

}

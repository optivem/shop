package com.mycompany.myshop.testkit.driver.adapter.api.client.controllers;

import com.mycompany.myshop.testkit.driver.port.dtos.BrowseOrderHistoryResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewOrderResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.PlaceOrderRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.PlaceOrderResponse;
import com.mycompany.myshop.testkit.driver.adapter.api.client.dtos.errors.ProblemDetailResponse;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.http.JsonHttpClient;
import com.mycompany.myshop.testkit.common.Result;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

    public Result<BrowseOrderHistoryResponse, ProblemDetailResponse> browseOrderHistory(String orderNumber) {
        var path = orderNumber == null || orderNumber.isBlank()
                ? ENDPOINT
                : ENDPOINT + "?orderNumber=" + URLEncoder.encode(orderNumber, StandardCharsets.UTF_8);
        return httpClient.get(path, BrowseOrderHistoryResponse.class);
    }

    public Result<Void, ProblemDetailResponse> cancelOrder(String orderNumber) {
        return httpClient.post(ENDPOINT + "/" + orderNumber + "/cancel");
    }

    public Result<Void, ProblemDetailResponse> deliverOrder(String orderNumber) {
        return httpClient.post(ENDPOINT + "/" + orderNumber + "/deliver");
    }

}

package com.mycompany.myshop.testkit.driver.adapter.api.client.controllers;

import com.mycompany.myshop.testkit.driver.port.dtos.BrowseOrderHistoryResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewOrderResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.PlaceOrderRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.PlaceOrderResponse;
import com.mycompany.myshop.testkit.driver.adapter.api.client.dtos.errors.ProblemDetailResponse;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.http.JsonHttpClient;
import com.mycompany.myshop.testkit.common.Result;

import java.util.LinkedHashMap;
import java.util.Map;

public class OrderController {
    private static final String ENDPOINT = "/api/orders";

    private final JsonHttpClient<ProblemDetailResponse> httpClient;

    public OrderController(JsonHttpClient<ProblemDetailResponse> httpClient) {
        this.httpClient = httpClient;
    }

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    public Result<PlaceOrderResponse, ProblemDetailResponse> placeOrder(PlaceOrderRequest request) {
        var idempotencyKey = request.getIdempotencyKey();
        if (idempotencyKey == null) {
            return httpClient.post(ENDPOINT, request, PlaceOrderResponse.class);
        }
        return httpClient.post(ENDPOINT, request, Map.of(IDEMPOTENCY_KEY_HEADER, idempotencyKey), PlaceOrderResponse.class);
    }

    public Result<ViewOrderResponse, ProblemDetailResponse> viewOrder(String orderNumber) {
        return httpClient.get(ENDPOINT + "/" + orderNumber, ViewOrderResponse.class);
    }

    public Result<BrowseOrderHistoryResponse, ProblemDetailResponse> browseOrderHistory(String orderNumber, String page, String size) {
        var query = new LinkedHashMap<String, String>();
        if (orderNumber != null && !orderNumber.isBlank()) {
            query.put("orderNumber", orderNumber);
        }
        query.put("page", page);
        query.put("size", size);
        return httpClient.get(ENDPOINT + QueryString.of(query), BrowseOrderHistoryResponse.class);
    }

    public Result<Void, ProblemDetailResponse> cancelOrder(String orderNumber) {
        return httpClient.post(ENDPOINT + "/" + orderNumber + "/cancel");
    }

    public Result<Void, ProblemDetailResponse> deliverOrder(String orderNumber) {
        return httpClient.post(ENDPOINT + "/" + orderNumber + "/deliver");
    }

}

package com.optivem.eshop.dsl.driver.adapter.shop.api.client;

import com.optivem.eshop.dsl.driver.adapter.shop.api.client.controllers.HealthController;
import com.optivem.eshop.dsl.driver.adapter.shop.api.client.controllers.OrderController;
import com.optivem.eshop.dsl.driver.adapter.shop.api.client.dtos.errors.ProblemDetailResponse;
import com.optivem.eshop.dsl.driver.adapter.shared.client.http.JsonHttpClient;
import com.optivem.eshop.dsl.common.Closer;

public class ShopApiClient implements AutoCloseable {
    private final JsonHttpClient<ProblemDetailResponse> httpClient;
    private final HealthController healthController;
    private final OrderController orderController;

    public ShopApiClient(String baseUrl) {
        this.httpClient = new JsonHttpClient<>(baseUrl, ProblemDetailResponse.class);
        this.healthController = new HealthController(httpClient);
        this.orderController = new OrderController(httpClient);
    }

    public HealthController health() {
        return healthController;
    }

    public OrderController orders() {
        return orderController;
    }

    @Override
    public void close() {
        Closer.close(httpClient);
    }
}




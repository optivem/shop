package com.optivem.shop.dsl.driver.adapter.shop.api.client;

import com.optivem.shop.dsl.driver.adapter.shop.api.client.controllers.CouponController;
import com.optivem.shop.dsl.driver.adapter.shop.api.client.controllers.HealthController;
import com.optivem.shop.dsl.driver.adapter.shop.api.client.controllers.OrderController;
import com.optivem.shop.dsl.driver.adapter.shop.api.client.dtos.errors.ProblemDetailResponse;
import com.optivem.shop.dsl.driver.adapter.shared.client.http.JsonHttpClient;
import com.optivem.shop.dsl.common.Closer;

public class ShopApiClient implements AutoCloseable {
    private final JsonHttpClient<ProblemDetailResponse> httpClient;
    private final HealthController healthController;
    private final OrderController orderController;
    private final CouponController couponController;

    public ShopApiClient(String baseUrl) {
        this.httpClient = new JsonHttpClient<>(baseUrl, ProblemDetailResponse.class);
        this.healthController = new HealthController(httpClient);
        this.orderController = new OrderController(httpClient);
        this.couponController = new CouponController(httpClient);
    }

    public HealthController health() {
        return healthController;
    }

    public OrderController orders() {
        return orderController;
    }

    public CouponController coupons() {
        return couponController;
    }

    @Override
    public void close() {
        Closer.close(httpClient);
    }
}

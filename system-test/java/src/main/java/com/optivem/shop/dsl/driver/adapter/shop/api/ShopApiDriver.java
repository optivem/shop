package com.optivem.shop.dsl.driver.adapter.shop.api;

import com.optivem.shop.dsl.driver.adapter.shop.api.client.ShopApiClient;
import com.optivem.shop.dsl.driver.port.shared.dtos.ErrorResponse;
import com.optivem.shop.dsl.driver.port.shop.dtos.BrowseCouponsResponse;
import com.optivem.shop.dsl.driver.port.shop.dtos.PlaceOrderRequest;
import com.optivem.shop.dsl.driver.port.shop.dtos.PlaceOrderResponse;
import com.optivem.shop.dsl.driver.port.shop.dtos.PublishCouponRequest;
import com.optivem.shop.dsl.driver.port.shop.dtos.ViewOrderResponse;
import com.optivem.shop.dsl.driver.port.shop.ShopDriver;
import com.optivem.shop.dsl.common.Closer;
import com.optivem.shop.dsl.common.Result;

public class ShopApiDriver implements ShopDriver {
    private final ShopApiClient apiClient;

    public ShopApiDriver(String baseUrl) {
        this.apiClient = new ShopApiClient(baseUrl);
    }

    @Override
    public void close() {
        Closer.close(apiClient);
    }

    @Override
    public Result<Void, ErrorResponse> goToShop() {
        return apiClient.health().checkHealth().mapError(SystemErrorMapper::from);
    }

    @Override
    public Result<PlaceOrderResponse, ErrorResponse> placeOrder(PlaceOrderRequest request) {
        return apiClient.orders().placeOrder(request).mapError(SystemErrorMapper::from);
    }

    @Override
    public Result<ViewOrderResponse, ErrorResponse> viewOrder(String orderNumber) {
        return apiClient.orders().viewOrder(orderNumber).mapError(SystemErrorMapper::from);
    }

    @Override
    public Result<Void, ErrorResponse> publishCoupon(PublishCouponRequest request) {
        return apiClient.coupons().publishCoupon(request).mapError(SystemErrorMapper::from);
    }

    @Override
    public Result<BrowseCouponsResponse, ErrorResponse> browseCoupons() {
        return apiClient.coupons().browseCoupons().mapError(SystemErrorMapper::from);
    }
}

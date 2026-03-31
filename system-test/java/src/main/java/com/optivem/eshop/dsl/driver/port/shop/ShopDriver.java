package com.optivem.eshop.dsl.driver.port.shop;

import com.optivem.eshop.dsl.driver.port.shared.dtos.ErrorResponse;
import com.optivem.eshop.dsl.driver.port.shop.dtos.PlaceOrderRequest;
import com.optivem.eshop.dsl.driver.port.shop.dtos.PlaceOrderResponse;
import com.optivem.eshop.dsl.driver.port.shop.dtos.ViewOrderResponse;
import com.optivem.eshop.dsl.common.Result;

public interface ShopDriver extends AutoCloseable {
    Result<Void, ErrorResponse> goToShop();

    Result<PlaceOrderResponse, ErrorResponse> placeOrder(PlaceOrderRequest request);

    Result<ViewOrderResponse, ErrorResponse> viewOrder(String orderNumber);

}

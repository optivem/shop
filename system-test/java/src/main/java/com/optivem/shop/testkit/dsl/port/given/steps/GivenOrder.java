package com.optivem.shop.testkit.dsl.port.given.steps;

import com.optivem.shop.testkit.dsl.port.given.steps.base.GivenStep;
import com.optivem.shop.testkit.driver.port.shop.dtos.OrderStatus;

public interface GivenOrder extends GivenStep {
    GivenOrder withOrderNumber(String orderNumber);

    GivenOrder withSku(String sku);

    GivenOrder withQuantity(String quantity);

    GivenOrder withQuantity(int quantity);

    GivenOrder withCountry(String country);

    GivenOrder withCouponCode(String couponCode);

    GivenOrder withStatus(String status);

    GivenOrder withStatus(OrderStatus status);
}

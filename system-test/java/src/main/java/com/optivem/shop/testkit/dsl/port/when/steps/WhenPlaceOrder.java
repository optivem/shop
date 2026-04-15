package com.optivem.shop.testkit.dsl.port.when.steps;

import com.optivem.shop.testkit.dsl.port.when.steps.base.WhenStep;

public interface WhenPlaceOrder extends WhenStep {
    WhenPlaceOrder withOrderNumber(String orderNumber);

    WhenPlaceOrder withSku(String sku);

    WhenPlaceOrder withQuantity(String quantity);

    WhenPlaceOrder withQuantity(int quantity);

    WhenPlaceOrder withCountry(String country);

    WhenPlaceOrder withCouponCode(String couponCode);

    WhenPlaceOrder withCouponCode();
}

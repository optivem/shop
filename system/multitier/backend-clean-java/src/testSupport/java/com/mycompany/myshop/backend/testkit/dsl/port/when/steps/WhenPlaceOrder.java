package com.mycompany.myshop.backend.testkit.dsl.port.when.steps;

import com.mycompany.myshop.backend.testkit.dsl.port.when.steps.base.WhenStep;

public interface WhenPlaceOrder extends WhenStep {
    WhenPlaceOrder withSku(String sku);

    WhenPlaceOrder withQuantity(int quantity);

    WhenPlaceOrder withQuantity(String quantity);

    WhenPlaceOrder withCountry(String country);

    WhenPlaceOrder withCouponCode(String couponCode);

    WhenPlaceOrder withCouponCode();
}

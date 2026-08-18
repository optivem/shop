package com.mycompany.myshop.backend.testkit.dsl.port.given.steps;

import com.mycompany.myshop.backend.domain.values.OrderStatus;
import com.mycompany.myshop.backend.testkit.dsl.port.given.steps.base.GivenStep;

public interface GivenOrder extends GivenStep {

    GivenOrder withOrderNumber(String orderNumberAlias);

    GivenOrder withSku(String sku);

    GivenOrder withQuantity(int quantity);

    GivenOrder withCountry(String country);

    GivenOrder withCouponCode(String couponCode);

    GivenOrder withStatus(OrderStatus status);
}

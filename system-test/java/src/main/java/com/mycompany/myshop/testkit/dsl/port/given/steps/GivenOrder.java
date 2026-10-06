package com.mycompany.myshop.testkit.dsl.port.given.steps;

import com.mycompany.myshop.testkit.dsl.port.given.steps.base.GivenStep;
import com.mycompany.myshop.testkit.common.domain.OrderStatus;

public interface GivenOrder extends GivenStep {
    GivenOrder withOrderNumber(String orderNumber);

    GivenOrder withSku(String sku);

    GivenOrder withQuantity(String quantity);

    GivenOrder withQuantity(int quantity);

    GivenOrder withCountry(String country);

    GivenOrder withCouponCode(String couponCode);

    /** The order is placed by the default customer (customer1). */
    GivenOrder placedByCustomer();

    /** The order is placed by the customer known in this scenario by the given alias. */
    GivenOrder placedByCustomer(String alias);

    GivenOrder withStatus(String status);

    GivenOrder withStatus(OrderStatus status);
}

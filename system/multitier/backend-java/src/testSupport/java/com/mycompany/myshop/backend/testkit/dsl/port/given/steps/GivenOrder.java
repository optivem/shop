package com.mycompany.myshop.backend.support.port.given.steps;

import com.mycompany.myshop.backend.support.port.given.steps.base.GivenStep;

/**
 * An order that was already placed before the scenario's own {@code when()} — a real {@code POST
 * /api/orders}, not a seeded row, so everything it touches on the way through (a coupon's usage
 * count, most of all) moves exactly as it would in production.
 *
 * <p>Unlike the system-test twin, it takes no order number: the SUT mints those, and a component
 * test has no business dictating one.
 */
public interface GivenOrder extends GivenStep {
    GivenOrder withSku(String sku);

    GivenOrder withQuantity(int quantity);

    GivenOrder withCountry(String country);

    GivenOrder withCouponCode(String couponCode);
}

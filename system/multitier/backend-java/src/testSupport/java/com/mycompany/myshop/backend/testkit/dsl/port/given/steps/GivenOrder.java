package com.mycompany.myshop.backend.testkit.dsl.port.given.steps;

import com.mycompany.myshop.backend.core.entities.OrderStatus;
import com.mycompany.myshop.backend.testkit.dsl.port.given.steps.base.GivenStep;

/**
 * An order that was already placed before the scenario's own {@code when()} — a real {@code POST
 * /api/orders}, not a seeded row, so everything it touches on the way through (a coupon's usage
 * count, most of all) moves exactly as it would in production.
 *
 * <p>Unlike the system-test twin, it takes no order number: the SUT mints those, and a component
 * test has no business dictating one. What a test may do is {@link #withOrderNumber(String) name}
 * the order, so a later step can refer to it — the name is an alias the DSL resolves to whatever the
 * SUT minted, not the number itself.
 */
public interface GivenOrder extends GivenStep {

    /**
     * Names this order so {@code when().cancelOrder().withOrderNumber(...)} can refer to it. Left
     * unset, the order takes {@code ScenarioDefaults.DEFAULT_ORDER_NUMBER} — which is also what the
     * cancel step defaults to, so the common single-order scenario needs no name at all.
     */
    GivenOrder withOrderNumber(String orderNumberAlias);

    GivenOrder withSku(String sku);

    GivenOrder withQuantity(int quantity);

    GivenOrder withCountry(String country);

    GivenOrder withCouponCode(String couponCode);

    /**
     * The status the order should already be in when the scenario's own {@code when()} runs. Reached
     * the way production would reach it — {@code PLACED} is the placement itself, {@code CANCELLED}
     * places and then cancels — not by writing the column.
     *
     * <p>{@code DELIVERED} is not supported: {@code POST /api/orders/{orderNumber}/deliver} has no
     * driver method yet, which is a deliberate non-goal of the plan that added cancellation.
     */
    GivenOrder withStatus(OrderStatus status);
}

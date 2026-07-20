package com.mycompany.myshop.backend.support.core.scenario;

/**
 * What the scenario DSL fills in when a test stays silent, so a test states only what it actually
 * depends on: {@code scenario.when().placeOrder().then().shouldSucceed()} works with no {@code
 * given()} at all.
 *
 * <p>{@link #DEFAULT_TIME} sits well clear of the New Year blackout window, so a scenario that never
 * mentions the clock is never rejected by it.
 */
public final class ScenarioDefaults {

    // Product
    public static final String DEFAULT_SKU = "DEFAULT-SKU";
    public static final String DEFAULT_UNIT_PRICE = "20.00";

    // Order
    public static final int DEFAULT_QUANTITY = 1;
    public static final String DEFAULT_COUNTRY = "US";

    // Promotion
    public static final boolean DEFAULT_PROMOTION_ACTIVE = false;
    public static final String DEFAULT_PROMOTION_DISCOUNT = "1.00";

    // Tax
    public static final String DEFAULT_TAX_RATE = "0.07";

    // Coupon
    public static final String DEFAULT_COUPON_CODE = "DEFAULT-COUPON";
    public static final String DEFAULT_DISCOUNT_RATE = "0.10";
    public static final int DEFAULT_USAGE_LIMIT = 1000;

    // Clock
    public static final String DEFAULT_TIME = "2025-12-24T10:00:00Z";

    public static final String EMPTY = null;

    private ScenarioDefaults() {
    }
}

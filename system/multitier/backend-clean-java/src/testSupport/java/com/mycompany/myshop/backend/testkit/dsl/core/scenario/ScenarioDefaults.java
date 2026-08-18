package com.mycompany.myshop.backend.testkit.dsl.core.scenario;

import com.mycompany.myshop.backend.domain.values.OrderStatus;

public final class ScenarioDefaults {

    // Product
    public static final String DEFAULT_SKU = "DEFAULT-SKU";
    public static final String DEFAULT_UNIT_PRICE = "20.00";

    // Order
    public static final int DEFAULT_QUANTITY = 1;
    public static final String DEFAULT_COUNTRY = "US";

    public static final String DEFAULT_ORDER_NUMBER = "DEFAULT-ORDER";

    public static final OrderStatus DEFAULT_ORDER_STATUS = OrderStatus.PLACED;

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

package com.optivem.eshop.dsl.core.scenario;

import com.optivem.eshop.dsl.driver.port.shop.dtos.OrderStatus;

/**
 * Default values for Gherkin test steps.
 * These defaults are used when test data is not explicitly specified.
 */
public final class ScenarioDefaults {

    // Product defaults
    public static final String DEFAULT_SKU = "DEFAULT-SKU";
    public static final String DEFAULT_UNIT_PRICE = "20.00";

    // Order defaults
    public static final String DEFAULT_ORDER_NUMBER = "DEFAULT-ORDER";
    public static final String DEFAULT_QUANTITY = "1";
    public static final OrderStatus DEFAULT_ORDER_STATUS = OrderStatus.PLACED;

    // Clock defaults
    public static final String DEFAULT_TIME = "2025-12-24T10:00:00Z";
    public static final String WEEKDAY_TIME = "2026-01-15T10:30:00Z";
    public static final String WEEKEND_TIME = "2026-01-17T10:30:00Z";

    public static final String EMPTY = "";

    private ScenarioDefaults() {
        // Prevent instantiation
    }
}


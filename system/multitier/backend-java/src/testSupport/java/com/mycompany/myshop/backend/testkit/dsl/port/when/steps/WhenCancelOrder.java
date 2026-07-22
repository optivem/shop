package com.mycompany.myshop.backend.testkit.dsl.port.when.steps;

import com.mycompany.myshop.backend.testkit.dsl.port.when.steps.base.WhenStep;

/**
 * Cancels an order placed by {@code given().order()}.
 *
 * <p>The one-scenario-per-test guard rules out placing and cancelling in the same {@code when()}, so
 * the order under test always comes from {@code given()}.
 */
public interface WhenCancelOrder extends WhenStep {

    /**
     * Which order to cancel, named by the alias {@code given().order()} registered it under. Defaults
     * to {@code ScenarioDefaults.DEFAULT_ORDER_NUMBER}, so a scenario with a single given order need
     * not name it. An unregistered name passes through as a literal — which is how a test names an
     * order that does not exist.
     */
    WhenCancelOrder withOrderNumber(String orderNumber);
}

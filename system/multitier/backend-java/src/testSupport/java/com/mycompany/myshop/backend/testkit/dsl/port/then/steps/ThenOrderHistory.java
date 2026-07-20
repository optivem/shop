package com.mycompany.myshop.backend.support.port.then.steps;

import com.mycompany.myshop.backend.support.port.then.steps.base.ThenStep;

/** Order history, read back through {@code GET /api/orders}. */
public interface ThenOrderHistory extends ThenStep<ThenOrderHistory> {
    ThenOrderHistory containsOrder(String expectedOrderNumber);

    /** Asserts the order the action placed shows up in the history. */
    ThenOrderHistory containsOrder();
}

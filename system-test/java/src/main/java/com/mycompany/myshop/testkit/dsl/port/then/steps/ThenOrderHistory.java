package com.mycompany.myshop.testkit.dsl.port.then.steps;

import com.mycompany.myshop.testkit.dsl.port.then.steps.base.ThenStep;

public interface ThenOrderHistory extends ThenStep<ThenOrderHistory> {
    ThenOrderHistory containsOrder(String orderNumber);

    ThenOrderHistory doesNotContainOrder(String orderNumber);
}

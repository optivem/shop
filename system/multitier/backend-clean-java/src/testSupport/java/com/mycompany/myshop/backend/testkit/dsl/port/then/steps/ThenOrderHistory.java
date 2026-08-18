package com.mycompany.myshop.backend.testkit.dsl.port.then.steps;

import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.base.ThenStep;

public interface ThenOrderHistory extends ThenStep<ThenOrderHistory> {
    ThenOrderHistory containsOrder(String expectedOrderNumber);

    ThenOrderHistory containsOrder();
}

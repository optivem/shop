package com.mycompany.myshop.testkit.dsl.port.then.steps;

import com.mycompany.myshop.testkit.dsl.port.then.steps.base.ThenStep;

public interface ThenCustomerList extends ThenStep<ThenCustomerList> {
    /**
     * Each of these customers (aliases) is listed once, in this order. Every customer who has ever ordered on the
     * shared stack is listed too, so others may come before, between or after them.
     */
    ThenCustomerList listsCustomersInOrder(String... customerAliases);
}

package com.mycompany.myshop.testkit.dsl.port.when.steps;

import com.mycompany.myshop.testkit.dsl.port.when.steps.base.WhenStep;

public interface WhenBrowseOrderHistory extends WhenStep {
    WhenBrowseOrderHistory withOrderNumber(String orderNumber);

    WhenBrowseOrderHistory withPage(String page);

    WhenBrowseOrderHistory withSize(String size);

    /** Filters by the default customer (customer1); only an admin's history is filtered. */
    WhenBrowseOrderHistory withCustomer();

    /** Filters by the customer known in this scenario by the given alias; only an admin's history is filtered. */
    WhenBrowseOrderHistory withCustomer(String customerAlias);
}

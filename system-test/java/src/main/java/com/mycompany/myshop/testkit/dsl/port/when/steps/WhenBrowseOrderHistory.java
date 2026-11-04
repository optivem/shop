package com.mycompany.myshop.testkit.dsl.port.when.steps;

import com.mycompany.myshop.testkit.dsl.port.when.steps.base.WhenStep;

public interface WhenBrowseOrderHistory extends WhenStep {
    WhenBrowseOrderHistory withOrderNumber(String orderNumber);

    WhenBrowseOrderHistory withPage(String page);

    WhenBrowseOrderHistory withSize(String size);
}

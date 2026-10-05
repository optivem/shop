package com.mycompany.myshop.testkit.dsl.port.when.steps;

import com.mycompany.myshop.testkit.dsl.port.when.steps.base.WhenStep;

public interface WhenDeliverOrder extends WhenStep {
    WhenDeliverOrder withOrderNumber(String orderNumber);
}

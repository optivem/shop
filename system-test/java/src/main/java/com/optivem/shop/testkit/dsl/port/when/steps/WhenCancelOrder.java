package com.optivem.shop.testkit.dsl.port.when.steps;

import com.optivem.shop.testkit.dsl.port.when.steps.base.WhenStep;

public interface WhenCancelOrder extends WhenStep {
    WhenCancelOrder withOrderNumber(String orderNumber);
}

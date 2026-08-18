package com.mycompany.myshop.backend.testkit.dsl.port.when.steps;

import com.mycompany.myshop.backend.testkit.dsl.port.when.steps.base.WhenStep;

public interface WhenCancelOrder extends WhenStep {

    WhenCancelOrder withOrderNumber(String orderNumber);
}

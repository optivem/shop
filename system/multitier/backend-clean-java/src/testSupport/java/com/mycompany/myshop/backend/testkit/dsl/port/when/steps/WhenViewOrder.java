package com.mycompany.myshop.backend.testkit.dsl.port.when.steps;

import com.mycompany.myshop.backend.testkit.dsl.port.when.steps.base.WhenStep;

public interface WhenViewOrder extends WhenStep {
    WhenViewOrder withOrderNumber(String orderNumber);
}

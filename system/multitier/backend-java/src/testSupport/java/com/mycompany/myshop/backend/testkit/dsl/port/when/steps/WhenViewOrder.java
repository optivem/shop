package com.mycompany.myshop.backend.support.port.when.steps;

import com.mycompany.myshop.backend.support.port.when.steps.base.WhenStep;

public interface WhenViewOrder extends WhenStep {
    WhenViewOrder withOrderNumber(String orderNumber);
}

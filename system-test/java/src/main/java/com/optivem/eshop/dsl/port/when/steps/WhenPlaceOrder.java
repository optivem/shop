package com.optivem.eshop.dsl.port.when.steps;

import com.optivem.eshop.dsl.port.when.steps.base.WhenStep;

public interface WhenPlaceOrder extends WhenStep {
    WhenPlaceOrder withOrderNumber(String orderNumber);

    WhenPlaceOrder withSku(String sku);

    WhenPlaceOrder withQuantity(String quantity);

    WhenPlaceOrder withQuantity(int quantity);
}


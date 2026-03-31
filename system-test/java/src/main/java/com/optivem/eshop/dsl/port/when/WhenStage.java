package com.optivem.eshop.dsl.port.when;

import com.optivem.eshop.dsl.port.when.steps.WhenPlaceOrder;
import com.optivem.eshop.dsl.port.when.steps.WhenViewOrder;

public interface WhenStage {
    WhenPlaceOrder placeOrder();

    WhenViewOrder viewOrder();
}



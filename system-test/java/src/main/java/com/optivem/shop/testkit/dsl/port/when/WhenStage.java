package com.optivem.shop.testkit.dsl.port.when;

import com.optivem.shop.testkit.dsl.port.when.steps.WhenBrowseCoupons;
import com.optivem.shop.testkit.dsl.port.when.steps.WhenCancelOrder;
import com.optivem.shop.testkit.dsl.port.when.steps.WhenPlaceOrder;
import com.optivem.shop.testkit.dsl.port.when.steps.WhenPublishCoupon;
import com.optivem.shop.testkit.dsl.port.when.steps.WhenViewOrder;

public interface WhenStage {
    WhenPlaceOrder placeOrder();

    WhenCancelOrder cancelOrder();

    WhenViewOrder viewOrder();

    WhenPublishCoupon publishCoupon();

    WhenBrowseCoupons browseCoupons();
}

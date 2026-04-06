package com.optivem.shop.dsl.port.when;

import com.optivem.shop.dsl.port.when.steps.WhenBrowseCoupons;
import com.optivem.shop.dsl.port.when.steps.WhenPlaceOrder;
import com.optivem.shop.dsl.port.when.steps.WhenPublishCoupon;
import com.optivem.shop.dsl.port.when.steps.WhenViewOrder;

public interface WhenStage {
    WhenPlaceOrder placeOrder();

    WhenViewOrder viewOrder();

    WhenPublishCoupon publishCoupon();

    WhenBrowseCoupons browseCoupons();
}

package com.mycompany.myshop.backend.testkit.dsl.port.when;

import com.mycompany.myshop.backend.testkit.dsl.port.when.steps.WhenBrowseCoupons;
import com.mycompany.myshop.backend.testkit.dsl.port.when.steps.WhenBrowseOrderHistory;
import com.mycompany.myshop.backend.testkit.dsl.port.when.steps.WhenCancelOrder;
import com.mycompany.myshop.backend.testkit.dsl.port.when.steps.WhenPlaceOrder;
import com.mycompany.myshop.backend.testkit.dsl.port.when.steps.WhenPublishCoupon;
import com.mycompany.myshop.backend.testkit.dsl.port.when.steps.WhenViewOrder;

/** The one action the scenario is about. */
public interface WhenStage {
    WhenPlaceOrder placeOrder();

    WhenCancelOrder cancelOrder();

    WhenViewOrder viewOrder();

    WhenBrowseOrderHistory browseOrderHistory();

    WhenPublishCoupon publishCoupon();

    WhenBrowseCoupons browseCoupons();
}

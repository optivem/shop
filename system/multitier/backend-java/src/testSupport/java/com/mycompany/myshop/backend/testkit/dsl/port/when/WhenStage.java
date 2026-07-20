package com.mycompany.myshop.backend.support.port.when;

import com.mycompany.myshop.backend.support.port.when.steps.WhenBrowseCoupons;
import com.mycompany.myshop.backend.support.port.when.steps.WhenBrowseOrderHistory;
import com.mycompany.myshop.backend.support.port.when.steps.WhenPlaceOrder;
import com.mycompany.myshop.backend.support.port.when.steps.WhenPublishCoupon;
import com.mycompany.myshop.backend.support.port.when.steps.WhenViewOrder;

/** The one action the scenario is about. */
public interface WhenStage {
    WhenPlaceOrder placeOrder();

    WhenViewOrder viewOrder();

    WhenBrowseOrderHistory browseOrderHistory();

    WhenPublishCoupon publishCoupon();

    WhenBrowseCoupons browseCoupons();
}

package com.mycompany.myshop.testkit.dsl.port.when;

import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenBrowseCoupons;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenBrowseOrderHistory;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenCancelOrder;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenDeliverOrder;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenPlaceOrder;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenPublishCoupon;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenViewOrder;

public interface WhenStage {
    WhenStage actingAsCustomer();

    WhenStage actingAsAnotherCustomer();

    WhenStage actingAsAdmin();

    WhenStage actingAsAnonymous();

    WhenPlaceOrder placeOrder();

    WhenCancelOrder cancelOrder();

    WhenDeliverOrder deliverOrder();

    WhenViewOrder viewOrder();

    WhenPublishCoupon publishCoupon();

    WhenBrowseCoupons browseCoupons();

    WhenBrowseOrderHistory browseOrderHistory();
}

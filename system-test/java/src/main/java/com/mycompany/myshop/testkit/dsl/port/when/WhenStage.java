package com.mycompany.myshop.testkit.dsl.port.when;

import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenBrowseCatalog;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenBrowseCoupons;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenBrowseOrderHistory;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenCancelOrder;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenDeliverOrder;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenPlaceOrder;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenPublishCoupon;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenViewOrder;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenUpdateProfile;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenViewProfile;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenViewProduct;

public interface WhenStage {
    WhenPlaceOrder placeOrder();

    WhenCancelOrder cancelOrder();

    WhenDeliverOrder deliverOrder();

    WhenViewOrder viewOrder();

    WhenPublishCoupon publishCoupon();

    WhenBrowseCoupons browseCoupons();

    WhenBrowseOrderHistory browseOrderHistory();

    WhenViewProfile viewProfile();

    WhenUpdateProfile updateProfile();

    WhenBrowseCatalog browseCatalog();

    WhenViewProduct viewProduct();
}

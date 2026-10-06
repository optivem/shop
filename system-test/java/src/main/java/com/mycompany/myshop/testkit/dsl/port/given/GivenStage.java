package com.mycompany.myshop.testkit.dsl.port.given;

import com.mycompany.myshop.testkit.dsl.port.given.steps.GivenClock;
import com.mycompany.myshop.testkit.dsl.port.given.steps.GivenCoupon;
import com.mycompany.myshop.testkit.dsl.port.given.steps.GivenCountry;
import com.mycompany.myshop.testkit.dsl.port.given.steps.GivenOrder;
import com.mycompany.myshop.testkit.dsl.port.given.steps.GivenProduct;
import com.mycompany.myshop.testkit.dsl.port.given.steps.GivenPromotion;
import com.mycompany.myshop.testkit.dsl.port.then.ThenStage;
import com.mycompany.myshop.testkit.dsl.port.when.WhenStage;

public interface GivenStage {
    GivenClock clock();

    GivenProduct product();

    GivenPromotion promotion();

    GivenOrder order();

    GivenCountry country();

    GivenCoupon coupon();

    GivenStage loggedInAsCustomer();

    GivenStage loggedInAsAnotherCustomer();

    GivenStage loggedInAsAdmin();

    GivenStage notLoggedIn();

    WhenStage when();

    ThenStage then();
}

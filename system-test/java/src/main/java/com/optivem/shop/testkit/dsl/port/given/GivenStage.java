package com.optivem.shop.testkit.dsl.port.given;

import com.optivem.shop.testkit.dsl.port.given.steps.GivenClock;
import com.optivem.shop.testkit.dsl.port.given.steps.GivenCoupon;
import com.optivem.shop.testkit.dsl.port.given.steps.GivenCountry;
import com.optivem.shop.testkit.dsl.port.given.steps.GivenOrder;
import com.optivem.shop.testkit.dsl.port.given.steps.GivenProduct;
import com.optivem.shop.testkit.dsl.port.given.steps.GivenPromotion;
import com.optivem.shop.testkit.dsl.port.then.ThenStage;
import com.optivem.shop.testkit.dsl.port.when.WhenStage;

public interface GivenStage {
    GivenClock clock();

    GivenProduct product();

    GivenPromotion promotion();

    GivenOrder order();

    GivenCountry country();

    GivenCoupon coupon();

    WhenStage when();

    ThenStage then();
}

package com.mycompany.myshop.backend.testkit.dsl.port.given;

import com.mycompany.myshop.backend.testkit.dsl.port.given.steps.GivenClock;
import com.mycompany.myshop.backend.testkit.dsl.port.given.steps.GivenCoupon;
import com.mycompany.myshop.backend.testkit.dsl.port.given.steps.GivenCountry;
import com.mycompany.myshop.backend.testkit.dsl.port.given.steps.GivenOrder;
import com.mycompany.myshop.backend.testkit.dsl.port.given.steps.GivenProduct;
import com.mycompany.myshop.backend.testkit.dsl.port.given.steps.GivenPromotion;
import com.mycompany.myshop.backend.testkit.dsl.port.then.ThenStage;
import com.mycompany.myshop.backend.testkit.dsl.port.when.WhenStage;

public interface GivenStage {
    GivenClock clock();

    GivenProduct product();

    GivenPromotion promotion();

    GivenCountry country();

    GivenCoupon coupon();

    GivenOrder order();

    WhenStage when();

    ThenStage then();
}

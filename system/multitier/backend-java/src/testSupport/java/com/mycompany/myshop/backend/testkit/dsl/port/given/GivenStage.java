package com.mycompany.myshop.backend.support.port.given;

import com.mycompany.myshop.backend.support.port.given.steps.GivenClock;
import com.mycompany.myshop.backend.support.port.given.steps.GivenCoupon;
import com.mycompany.myshop.backend.support.port.given.steps.GivenCountry;
import com.mycompany.myshop.backend.support.port.given.steps.GivenOrder;
import com.mycompany.myshop.backend.support.port.given.steps.GivenProduct;
import com.mycompany.myshop.backend.support.port.given.steps.GivenPromotion;
import com.mycompany.myshop.backend.support.port.then.ThenStage;
import com.mycompany.myshop.backend.support.port.when.WhenStage;

/**
 * The world the scenario runs in. Each step is translated into use case calls — the ERP / Tax /
 * Clock steps program the WireMock stubs, {@code coupon()} publishes through the SUT's own API.
 *
 * <p>Unstated values are filled from {@code ScenarioDefaults}, so a scenario states only what it
 * actually depends on.
 */
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

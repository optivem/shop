package com.optivem.shop.dsl.port.given.steps;

import com.optivem.shop.dsl.port.given.steps.base.GivenStep;

public interface GivenCoupon extends GivenStep {
    GivenCoupon withCode(String code);
    GivenCoupon withDiscountRate(double discountRate);
    GivenCoupon withDiscountRate(String discountRate);
}

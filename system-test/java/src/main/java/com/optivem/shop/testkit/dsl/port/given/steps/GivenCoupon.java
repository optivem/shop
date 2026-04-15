package com.optivem.shop.testkit.dsl.port.given.steps;

import com.optivem.shop.testkit.dsl.port.given.steps.base.GivenStep;

public interface GivenCoupon extends GivenStep {
    GivenCoupon withCouponCode(String couponCode);

    GivenCoupon withDiscountRate(String discountRate);

    GivenCoupon withDiscountRate(double discountRate);

    GivenCoupon withValidFrom(String validFrom);

    GivenCoupon withValidTo(String validTo);

    GivenCoupon withUsageLimit(String usageLimit);

    GivenCoupon withUsageLimit(int usageLimit);
}

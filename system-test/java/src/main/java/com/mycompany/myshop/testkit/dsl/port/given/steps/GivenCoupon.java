package com.mycompany.myshop.testkit.dsl.port.given.steps;

import com.mycompany.myshop.testkit.dsl.port.given.steps.base.GivenStep;

public interface GivenCoupon extends GivenStep {
    GivenCoupon withCouponCode(String couponCode);

    GivenCoupon withDiscountRate(String discountRate);

    GivenCoupon withValidFrom(String validFrom);

    GivenCoupon withValidTo(String validTo);

    GivenCoupon withUsageLimit(String usageLimit);

    GivenCoupon withUsageLimit(int usageLimit);
}

package com.mycompany.myshop.backend.support.port.given.steps;

import com.mycompany.myshop.backend.support.port.given.steps.base.GivenStep;

public interface GivenCoupon extends GivenStep {
    GivenCoupon withCouponCode(String couponCode);

    GivenCoupon withDiscountRate(String discountRate);

    GivenCoupon withDiscountRate(double discountRate);

    GivenCoupon withValidFrom(String validFrom);

    GivenCoupon withValidTo(String validTo);

    GivenCoupon withUsageLimit(int usageLimit);
}

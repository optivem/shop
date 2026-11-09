package com.mycompany.myshop.testkit.dsl.port.then.steps;

import com.mycompany.myshop.testkit.dsl.port.then.steps.base.ThenStep;

public interface ThenCoupon extends ThenStep<ThenCoupon> {
    ThenCoupon hasDiscountRate(String discountRate);

    ThenCoupon isValidFrom(String validFrom);

    ThenCoupon isValidTo(String validTo);

    ThenCoupon hasUsageLimit(int usageLimit);

    /** The coupon has no valid-from time, so it is valid immediately. */
    ThenCoupon isValidImmediately();

    /** The coupon has no valid-to time, so it never expires. */
    ThenCoupon neverExpires();

    /** The coupon has no usage limit. */
    ThenCoupon hasUnlimitedUsage();

    ThenCoupon hasUsedCount(int expectedUsedCount);
}

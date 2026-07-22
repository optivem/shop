package com.mycompany.myshop.testkit.dsl.port.then.steps;

import com.mycompany.myshop.testkit.dsl.port.then.steps.base.ThenStep;

public interface ThenCoupon extends ThenStep<ThenCoupon> {
    ThenCoupon hasDiscountRate(double discountRate);

    ThenCoupon hasDiscountRate(String discountRate);

    ThenCoupon isValidFrom(String validFrom);

    ThenCoupon isValidTo(String validTo);

    ThenCoupon hasUsageLimit(int usageLimit);

    ThenCoupon hasUsedCount(int expectedUsedCount);
}

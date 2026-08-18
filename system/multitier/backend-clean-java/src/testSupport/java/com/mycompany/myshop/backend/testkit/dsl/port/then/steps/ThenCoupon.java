package com.mycompany.myshop.backend.testkit.dsl.port.then.steps;

import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.base.ThenStep;

public interface ThenCoupon extends ThenStep<ThenCoupon> {
    ThenCoupon hasDiscountRate(String expectedDiscountRate);

    ThenCoupon hasDiscountRate(double expectedDiscountRate);

    ThenCoupon hasUsageLimit(int expectedUsageLimit);

    ThenCoupon hasUsedCount(int expectedUsedCount);
}

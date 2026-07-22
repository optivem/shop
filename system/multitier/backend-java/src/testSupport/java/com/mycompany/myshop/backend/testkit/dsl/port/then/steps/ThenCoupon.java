package com.mycompany.myshop.backend.testkit.dsl.port.then.steps;

import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.base.ThenStep;

/**
 * A published coupon, read back through {@code GET /api/coupons}. Reaching the step at all asserts
 * the coupon exists.
 */
public interface ThenCoupon extends ThenStep<ThenCoupon> {
    ThenCoupon hasDiscountRate(String expectedDiscountRate);

    ThenCoupon hasDiscountRate(double expectedDiscountRate);

    ThenCoupon hasUsageLimit(int expectedUsageLimit);

    ThenCoupon hasUsedCount(int expectedUsedCount);
}

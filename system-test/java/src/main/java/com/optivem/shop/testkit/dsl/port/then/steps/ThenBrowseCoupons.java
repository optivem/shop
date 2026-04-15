package com.optivem.shop.testkit.dsl.port.then.steps;

import com.optivem.shop.testkit.dsl.port.then.steps.base.ThenStep;

public interface ThenBrowseCoupons extends ThenStep<ThenBrowseCoupons> {
    ThenBrowseCoupons containsCouponWithCode(String expectedCode);

    ThenBrowseCoupons couponCount(int expectedCount);
}

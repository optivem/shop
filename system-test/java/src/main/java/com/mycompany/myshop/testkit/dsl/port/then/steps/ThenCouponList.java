package com.mycompany.myshop.testkit.dsl.port.then.steps;

import com.mycompany.myshop.testkit.dsl.port.then.steps.base.ThenStep;

public interface ThenCouponList extends ThenStep<ThenCouponList> {
    /**
     * The list begins with these coupons (aliases), in this order. The coupons table is shared, so coupons published
     * before the scenario may follow them; a scenario that relies on its coupons being the newest must run isolated.
     */
    ThenCouponList startsWithCoupons(String... couponCodes);
}

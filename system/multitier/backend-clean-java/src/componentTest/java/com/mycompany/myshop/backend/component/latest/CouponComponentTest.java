package com.mycompany.myshop.backend.component.latest;

import com.mycompany.myshop.backend.BaseComponentTest;
import org.junit.jupiter.api.Test;

class CouponComponentTest extends BaseComponentTest {

    @Test
    void publishReturnsNoContentThenBrowseListsCoupon() {
        scenario.when().publishCoupon()
                .withCouponCode("SAVE10").withDiscountRate("0.20").withUsageLimit(100)
            .then().shouldSucceed()
            .and().coupon("SAVE10")
                .hasDiscountRate("0.20")
                .hasUsageLimit(100)
                .hasUsedCount(0);
    }
}

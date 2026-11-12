package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;

class BrowseCouponsPositiveTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldBeAbleToBrowseCoupons() {
        scenario
                .when().browseCoupons()
                .then().shouldSucceed();
    }

    @Tag("UC-005.4")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldShowHowManyTimesACouponHasBeenUsed() {
        scenario
                .given().coupon()
                    .withCouponCode("LIMITED2026")
                    .withUsageLimit(2)
                .and().order()
                    .withOrderNumber("ORD-1")
                    .withCouponCode("LIMITED2026")
                .and().order()
                    .withOrderNumber("ORD-2")
                    .withCouponCode("LIMITED2026")
                .when().browseCoupons()
                .then().shouldSucceed()
                .and().coupon("LIMITED2026")
                    .hasUsedCount(2);
    }
}

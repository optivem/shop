package com.optivem.shop.systemtest.latest.acceptance;

import com.optivem.shop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.optivem.shop.dsl.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;

class BrowseCouponsPositiveTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldBeAbleToBrowseCoupons() {
        scenario
                .when().browseCoupons()
                .then().shouldSucceed();
    }

    @TestTemplate
    @Channel(ChannelType.API)
    void publishedCouponShouldAppearInList() {
        scenario
                .given().coupon()
                    .withCouponCode("BROWSE10")
                    .withDiscountRate(0.10)
                .when().browseCoupons()
                .then().shouldSucceed()
                    .coupons()
                        .containsCouponWithCode("BROWSE10");
    }
}

package com.optivem.shop.systemtest.latest.acceptance;

import com.optivem.shop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.optivem.testing.Channel;
import com.optivem.shop.dsl.channel.ChannelType;

import org.junit.jupiter.api.TestTemplate;

class PublishCouponPositiveTest extends BaseAcceptanceTest {

    @TestTemplate
    @Channel(ChannelType.API)
    void shouldPublishCouponSuccessfully() {
        scenario
                .when().publishCoupon()
                    .withCode("SAVE10")
                    .withDiscountRate(0.10)
                .then().shouldSucceed();
    }
}

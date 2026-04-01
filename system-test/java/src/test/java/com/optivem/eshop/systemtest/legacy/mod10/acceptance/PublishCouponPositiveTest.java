package com.optivem.eshop.systemtest.legacy.mod10.acceptance;

import com.optivem.eshop.systemtest.legacy.mod10.acceptance.base.BaseAcceptanceTest;
import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.testing.Channel;

import org.junit.jupiter.api.TestTemplate;

class PublishCouponPositiveTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel({ ChannelType.UI, ChannelType.API })
    void shouldBeAbleToPublishValidCoupon() {
        scenario
                .when().publishCoupon()
                    .withCouponCode("SUMMER2025")
                    .withDiscountRate(0.15)
                    .withValidFrom("2024-06-01T00:00:00Z")
                    .withValidTo("2024-08-31T23:59:59Z")
                    .withUsageLimit(100)
                .then().shouldSucceed();
    }

    @TestTemplate
    @Channel({ ChannelType.UI, ChannelType.API })
    void shouldBeAbleToPublishCouponWithEmptyOptionalFields() {
        scenario
                .when().publishCoupon()
                .withCouponCode("SUMMER2025")
                .withDiscountRate(0.15)
                .withValidFrom("")
                .withValidTo("")
                .withUsageLimit("")
                .then().shouldSucceed();
    }

    @TestTemplate
    @Channel({ ChannelType.UI, ChannelType.API })
    void shouldBeAbleToCorrectlySaveCoupon() {
        scenario
                .when().publishCoupon()
                .withCouponCode("SUMMER2025")
                .withDiscountRate(0.15)
                .withValidFrom("2024-06-01T00:00:00Z")
                .withValidTo("2024-08-31T23:59:00Z")
                .withUsageLimit(100)
                .then().shouldSucceed().and().coupon("SUMMER2025")
                .hasDiscountRate(0.15)
                .isValidFrom("2024-06-01T00:00:00Z")
                .isValidTo("2024-08-31T23:59:00Z")
                .hasUsageLimit(100)
                .hasUsedCount(0);
    }
}



package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import com.optivem.testing.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;

class PublishCouponPositiveTest extends BaseAcceptanceTest {
    @Tag("UC-004.2")
    @Tag("UC-004.3")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldBeAbleToPublishValidCoupon() {
        scenario
                .when().publishCoupon()
                    .withCouponCode("SUMMER2025")
                    .withDiscountRate("0.15")
                    .withValidFrom("2024-06-01T00:00:00Z")
                    .withValidTo("2024-08-31T23:59:59Z")
                    .withUsageLimit(100)
                .then().shouldSucceed();
    }

    @Tag("UC-004.2")
    @Tag("UC-004.3")
    @Tag("UC-005.2")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldBeAbleToPublishCouponWithEmptyOptionalFields() {
        scenario
                .when().publishCoupon()
                    .withCouponCode("SUMMER2025")
                    .withDiscountRate("0.15")
                    .withValidFrom(null)
                    .withValidTo(null)
                    .withUsageLimit(null)
                .then().shouldSucceed()
                .and().coupon("SUMMER2025")
                    .isValidImmediately()
                    .neverExpires()
                    .hasUnlimitedUsage();
    }

    @Tag("UC-004.2")
    @Tag("UC-004.3")
    @Tag("UC-004.4")
    @Tag("UC-005.2")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldBeAbleToCorrectlySaveCoupon() {
        scenario
                .when().publishCoupon()
                    .withCouponCode("SUMMER2025")
                    .withDiscountRate("0.15")
                    .withValidFrom("2024-06-01T00:00:00Z")
                    .withValidTo("2024-08-31T23:59:00Z")
                    .withUsageLimit(100)
                .then().shouldSucceed()
                .and().coupon("SUMMER2025")
                    .hasDiscountRate("0.15")
                    .isValidFrom("2024-06-01T00:00:00Z")
                    .isValidTo("2024-08-31T23:59:00Z")
                    .hasUsageLimit(100)
                    .hasUsedCount(0);
    }

    @TestTemplate
    @Channel(ChannelType.API)
    void shouldPublishCouponSuccessfully() {
        scenario
                .when().publishCoupon()
                    .withCouponCode("SAVE10")
                    .withDiscountRate("0.10")
                .then().shouldSucceed();
    }

    @Tag("UC-004.1")
    @TestTemplate
    @Channel(value = {ChannelType.API}, alsoForFirstRow = ChannelType.UI)
    @DataSource({"0.01"})
    @DataSource({"0.15"})
    @DataSource({"1.00"})
    void shouldAcceptDiscountRateAboveZeroAndUpToOne(String discountRate) {
        scenario
                .when().publishCoupon()
                    .withCouponCode("SAVE")
                    .withDiscountRate(discountRate)
                .then().shouldSucceed()
                .and().coupon("SAVE")
                    .hasDiscountRate(discountRate);
    }
}

package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.mycompany.myshop.testkit.dsl.port.given.GivenStage;
import com.optivem.testing.Channel;
import com.optivem.testing.DataSource;
import com.optivem.testing.Isolated;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;

/**
 * The coupon list, newest first. The coupons table is shared and never emptied, so these tests check that the
 * scenario's coupons come first, in order: true only while no other test publishes a coupon. Not covered: the total
 * of 3 and the empty page 3 of @UC-005.3, and @UC-005.7 (no coupon published), which need a coupon set of their own.
 */
@Isolated("checks that its coupons are the newest; a coupon published in parallel would come before them")
class BrowseCouponsPositiveIsolatedTest extends BaseAcceptanceTest {
    private GivenStage givenThreeCouponsPublishedInOrder() {
        return scenario
                .given().coupon()
                    .withCouponCode("SUMMER2025")
                    .withDiscountRate("0.15")
                    .withValidFrom("2025-06-01T00:00:00Z")
                    .withValidTo("2025-08-31T23:59:59Z")
                    .withUsageLimit(100)
                .and().coupon()
                    .withCouponCode("SAVE10")
                    .withDiscountRate("0.10")
                .and().coupon()
                    .withCouponCode("LIMITED2026")
                    .withDiscountRate("0.20")
                    .withValidFrom("2026-01-01T00:00:00Z")
                    .withUsageLimit(2)
                .and().loggedInAsAdmin();
    }

    @Tag("UC-005.1")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldListEveryCouponNewestFirst() {
        givenThreeCouponsPublishedInOrder()
                .when().browseCoupons()
                .then().shouldSucceed()
                    .couponList()
                        .startsWithCoupons("LIMITED2026", "SAVE10", "SUMMER2025");
    }

    // Page 2 also holds the coupons published before the scenario, after SUMMER2025.
    @Tag("UC-005.3")
    @TestTemplate
    @Channel(ChannelType.API)
    @DataSource({"1", "LIMITED2026,SAVE10"})
    @DataSource({"2", "SUMMER2025"})
    void shouldReadTheCouponsOnePageAtATime(String page, String coupons) {
        givenThreeCouponsPublishedInOrder()
                .when().browseCoupons()
                    .withPage(page)
                    .withSize("2")
                .then().shouldSucceed()
                    .couponList()
                        .startsWithCoupons(coupons.split(","));
    }
}

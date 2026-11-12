package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import com.optivem.testing.Isolated;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;

@Isolated
class PlaceOrderPositiveIsolatedTest extends BaseAcceptanceTest {
    // The UI reads the coupon back from the coupon list, newest first with no paging: coupons published in parallel
    // would push it off the page.
    @Tag("UC-001.5")
    @Tag("UC-005.4")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void couponUsageCountHasBeenIncrementedAfterItsBeenUsed() {
        scenario
                .given().coupon()
                    .withCouponCode("SUMMER2025")
                .when().placeOrder()
                    .withCouponCode("SUMMER2025")
                .then().shouldSucceed()
                .and().coupon("SUMMER2025")
                    .hasUsedCount(1);
    }

    @Tag("UC-001.6")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldRecordPlacementTimestamp() {
        scenario
                .given().clock()
                    .withTime("2026-01-15T10:30:00Z")
                .when().placeOrder()
                .then().shouldSucceed()
                .and().clock()
                    .hasTime("2026-01-15T10:30:00Z");
    }

    @Tag("UC-001.7")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldApplyFullPriceWithoutPromotion() {
        scenario
                .given().product().withUnitPrice("20.00")
                .and().promotion().withActive(false)
                .and().country().withTaxRate("0.00")
                .when().placeOrder().withQuantity(5)
                .then().shouldSucceed()
                .and().order()
                    .hasTotalPrice("100.00");
    }

    @Tag("UC-001.7")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldApplyDiscountWhenPromotionIsActive() {
        scenario
                .given().product().withUnitPrice("20.00")
                .and().promotion().withActive(true).withDiscount("0.5")
                .and().country().withTaxRate("0.00")
                .when().placeOrder().withQuantity(5)
                .then().shouldSucceed()
                .and().order()
                    .hasTotalPrice("50.00");
    }
}

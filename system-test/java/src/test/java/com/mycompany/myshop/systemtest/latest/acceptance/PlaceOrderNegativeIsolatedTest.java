package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import com.optivem.testing.Isolated;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;

@Isolated("mutates the shared wall clock to year-end and coupon-expiry times; parallel runs would clash on the clock")
class PlaceOrderNegativeIsolatedTest extends BaseAcceptanceTest {
    @Tag("UC-001.13")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldRejectOrderPlacedAtYearEnd() {
        scenario
                .given().clock()
                    .withTime("2026-12-31T23:59:30Z")
                .when().placeOrder()
                .then().shouldFail()
                    .errorMessage("Orders cannot be placed between 23:59 and 00:00 on December 31st");
    }

    @Tag("UC-001.11")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void cannotPlaceOrderWithExpiredCoupon() {
        scenario
                .given().clock()
                    .withTime("2023-09-01T12:00:00Z")
                .and().coupon()
                    .withCouponCode("SUMMER2023")
                    .withValidFrom("2023-06-01T00:00:00Z")
                    .withValidTo("2023-08-31T23:59:59Z")
                .when().placeOrder()
                    .withCouponCode("SUMMER2023")
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("couponCode", "Coupon code SUMMER2023 has expired");
    }
}

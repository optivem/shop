package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import com.optivem.testing.Isolated;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;

@Isolated("mutates the shared wall clock to year-end and coupon-expiry times, and takes the shared ERP stub down or makes it slow; parallel runs would clash on the clock and the ERP")
class PlaceOrderNegativeIsolatedTest extends BaseAcceptanceTest {
    // A down ERP answers at once, so only the retry backoff (well under a second) delays the 503.
    private static final int ERP_DOWN_FAIL_FAST_SECONDS = 5;
    // A slow ERP costs up to 3 attempts x 2s timeout plus backoff; the stubbed ERP would take 20s.
    private static final int ERP_SLOW_FAIL_FAST_SECONDS = 12;

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

    @Tag("UC-001.14")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldRejectOrderWhenErpIsUnavailable() {
        scenario
                .given().erp()
                    .unavailable()
                .when().placeOrder()
                .then().shouldFail()
                    .errorMessage("Product information can't be retrieved right now");
    }

    // Resilience: the system fails fast with 503 + Retry-After instead of hanging or guessing a price.
    // API only, because the assertions are about the HTTP response.
    @Tag("UC-001.14")
    @TestTemplate
    @Channel({ChannelType.API})
    void shouldFailFastWithServiceUnavailableWhenErpIsDown() {
        scenario
                .given().erp()
                    .unavailable()
                .when().placeOrder()
                .then().shouldFail()
                    .errorMessage("Product information can't be retrieved right now")
                    .serviceUnavailable()
                    .failedWithinSeconds(ERP_DOWN_FAIL_FAST_SECONDS);
    }

    // The slow ERP answers only after 20s, far past the 2s outbound timeout: the call must give up on its
    // own (timeout times retries) well before the ERP would have answered.
    @Tag("UC-001.14")
    @TestTemplate
    @Channel({ChannelType.API})
    void shouldFailFastWithServiceUnavailableWhenErpIsSlow() {
        scenario
                .given().erp()
                    .slow()
                .when().placeOrder()
                .then().shouldFail()
                    .errorMessage("Product information can't be retrieved right now")
                    .serviceUnavailable()
                    .failedWithinSeconds(ERP_SLOW_FAIL_FAST_SECONDS);
    }
}

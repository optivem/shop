package com.optivem.eshop.systemtest.legacy.mod10.acceptance;

import com.optivem.eshop.systemtest.legacy.mod10.acceptance.base.BaseAcceptanceTest;
import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.testing.Channel;
import com.optivem.testing.Isolated;
import com.optivem.testing.TimeDependent;
import org.junit.jupiter.api.TestTemplate;

@Isolated
class PlaceOrderNegativeIsolatedTest extends BaseAcceptanceTest {
    @TimeDependent
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



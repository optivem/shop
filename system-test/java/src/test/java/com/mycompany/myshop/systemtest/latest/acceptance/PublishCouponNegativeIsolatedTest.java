package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import com.optivem.testing.Isolated;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;

@Isolated("the UI reads the coupon back from the coupon list, newest first with no paging; coupons published in parallel would push it off the page")
class PublishCouponNegativeIsolatedTest extends BaseAcceptanceTest {
    @Tag("UC-004.9")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void cannotPublishCouponWithDuplicateCouponCode() {
        scenario
                .given().coupon()
                    .withCouponCode("EXISTING-COUPON")
                    .withDiscountRate("0.10")
                .when().publishCoupon()
                    .withCouponCode("EXISTING-COUPON")
                    .withDiscountRate("0.20")
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("couponCode", "Coupon code EXISTING-COUPON already exists")
                .and().coupon("EXISTING-COUPON")
                    .hasDiscountRate("0.10");
    }
}

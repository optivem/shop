package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.commons.providers.EmptyArgumentsProvider;
import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.systemtest.latest.acceptance.base.RequiresKeycloak;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

class PublishCouponNegativeTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel(value = {ChannelType.API}, alsoForFirstRow = ChannelType.UI)
    @ValueSource(strings = {"0.0", "-0.01", "-0.15"})
    void cannotPublishCouponWithZeroOrNegativeDiscount(String discountRate) {
        scenario
                .when().publishCoupon()
                    .withCouponCode("INVALID-COUPON")
                    .withDiscountRate(discountRate)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("discountRate", "Discount rate must be greater than 0.00");
    }

    @TestTemplate
    @Channel(value = {ChannelType.API}, alsoForFirstRow = ChannelType.UI)
    @ValueSource(strings = {"1.01", "2.00"})
    void cannotPublishCouponWithDiscountGreaterThan100percent(String discountRate) {
        scenario
                .when().publishCoupon()
                    .withCouponCode("INVALID-COUPON")
                    .withDiscountRate(discountRate)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("discountRate", "Discount rate must be at most 1.00");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void cannotPublishCouponWithDuplicateCouponCode() {
        scenario
                .given().coupon()
                    .withCouponCode("EXISTING-COUPON")
                    .withDiscountRate(0.10)
                .when().publishCoupon()
                    .withCouponCode("EXISTING-COUPON")
                    .withDiscountRate(0.20)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("couponCode", "Coupon code EXISTING-COUPON already exists");
    }

    @TestTemplate
    @Channel(value = {ChannelType.API}, alsoForFirstRow = ChannelType.UI)
    @ValueSource(strings = {"0", "-1", "-100"})
    void cannotPublishCouponWithZeroOrNegativeUsageLimit(String usageLimit) {
        scenario
                .when().publishCoupon()
                    .withCouponCode("INVALID-LIMIT")
                    .withDiscountRate(0.15)
                    .withUsageLimit(usageLimit)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("usageLimit", "Usage limit must be positive");
    }

    @TestTemplate
    @Channel(ChannelType.API)
    @ArgumentsSource(EmptyArgumentsProvider.class)
    void shouldRejectCouponWithBlankCode(String code) {
        scenario
                .when().publishCoupon()
                    .withCouponCode(code)
                    .withDiscountRate(0.10)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("code", "Coupon code must not be blank");
    }

    @TestTemplate
    @Channel(ChannelType.API)
    @RequiresKeycloak
    void customerShouldNotBeAbleToPublishCoupon() {
        scenario
                .when().actingAsCustomer().publishCoupon()
                .then().shouldFail()
                    .statusCode(403);
    }
}
